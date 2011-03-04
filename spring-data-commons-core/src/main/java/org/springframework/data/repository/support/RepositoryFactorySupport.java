/*
 * Copyright 2008-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.support;

import static org.springframework.util.ReflectionUtils.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;


/**
 * Factory bean to create instances of a given repository interface. Creates a
 * proxy implementing the configured repository interface and apply an advice
 * handing the control to the {@code QueryExecuterMethodInterceptor}. Query
 * detection strategy can be configured by setting
 * {@link QueryLookupStrategy.Key}.
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryFactorySupport {

    private final List<RepositoryProxyPostProcessor> postProcessors =
            new ArrayList<RepositoryProxyPostProcessor>();
    private QueryLookupStrategy.Key queryLookupStrategyKey;


    /**
     * Sets the strategy of how to lookup a query to execute finders.
     * 
     * @param queryLookupStrategy the createFinderQueries to set
     */
    public void setQueryLookupStrategyKey(Key key) {

        this.queryLookupStrategyKey = key;
    }


    /**
     * Adds {@link RepositoryProxyPostProcessor}s to the factory to allow
     * manipulation of the {@link ProxyFactory} before the proxy gets created.
     * Note that the {@link QueryExecuterMethodInterceptor} will be added to the
     * proxy <em>after</em> the {@link RepositoryProxyPostProcessor}s are
     * considered.
     * 
     * @param processor
     */
    protected void addRepositoryProxyPostProcessor(
            RepositoryProxyPostProcessor processor) {

        Assert.notNull(processor);
        this.postProcessors.add(processor);
    }


    /**
     * Returns a repository instance for the given interface.
     * 
     * @param <T>
     * @param repositoryInterface
     * @return
     */
    public <T extends Repository<?, ?>> T getRepository(
            Class<T> repositoryInterface) {

        return getRepository(repositoryInterface, null);
    }


    /**
     * Returns a repository instance for the given interface backed by an
     * instance providing implementation logic for custom logic.
     * 
     * @param <T>
     * @param repositoryInterface
     * @param customImplementation
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    public <T> T getRepository(Class<T> repositoryInterface,
            Object customImplementation) {

        RepositoryMetadata metadata =
                new DefaultRepositoryMetadata(repositoryInterface,
                        getRepositoryBaseClass(repositoryInterface));

        validate(metadata, customImplementation);

        Object target = getTargetRepository(metadata);

        // Create proxy
        ProxyFactory result = new ProxyFactory();
        result.setTarget(target);
        result.setInterfaces(new Class[] { repositoryInterface });

        for (RepositoryProxyPostProcessor processor : postProcessors) {
            processor.postProcess(result);
        }

        result.addAdvice(new QueryExecuterMethodInterceptor(metadata,
                customImplementation, target));

        return (T) result.getProxy();
    }


    /**
     * Create a repository instance as backing for the query proxy.
     * 
     * @param domainClass
     * @return
     */
    protected abstract Object getTargetRepository(RepositoryMetadata metadata);


    /**
     * Returns the base class backing the actual repository instance. Make sure
     * {@link #getTargetRepository(RepositoryMetadata)} returns an instance of
     * this class.
     * 
     * @param repositoryInterface
     * @return
     */
    protected abstract Class<?> getRepositoryBaseClass(
            Class<?> repositoryInterface);


    /**
     * Returns the {@link QueryLookupStrategy} for the given {@link Key}.
     * 
     * @param key can be {@literal null}
     * @return
     */
    protected abstract QueryLookupStrategy getQueryLookupStrategy(Key key);


    /**
     * Validates the given repository interface as well as the given custom
     * implementation.
     * 
     * @param repositoryMetadata
     * @param customImplementation
     */
    protected void validate(RepositoryMetadata repositoryMetadata,
            Object customImplementation) {

        if (null == customImplementation
                && repositoryMetadata.hasCustomMethod()) {

            throw new IllegalArgumentException(
                    String.format(
                            "You have custom methods in %s but not provided a custom implementation!",
                            repositoryMetadata.getRepositoryInterface()));
        }
    }

    /**
     * This {@code MethodInterceptor} intercepts calls to methods of the custom
     * implementation and delegates the to it if configured. Furthermore it
     * resolves method calls to finders and triggers execution of them. You can
     * rely on having a custom repository implementation instance set if this
     * returns true.
     * 
     * @author Oliver Gierke
     */
    public class QueryExecuterMethodInterceptor implements MethodInterceptor {

        private final Map<Method, RepositoryQuery> queries =
                new ConcurrentHashMap<Method, RepositoryQuery>();

        private final Object customImplementation;
        private final RepositoryMetadata metadata;
        private final Object target;


        /**
         * Creates a new {@link QueryExecuterMethodInterceptor}. Builds a model
         * of {@link QueryMethod}s to be invoked on execution of repository
         * interface methods.
         */
        public QueryExecuterMethodInterceptor(
                RepositoryMetadata repositoryInterface,
                Object customImplementation, Object target) {

            this.metadata = repositoryInterface;
            this.customImplementation = customImplementation;
            this.target = target;

            QueryLookupStrategy lookupStrategy =
                    getQueryLookupStrategy(queryLookupStrategyKey);

            for (Method method : metadata.getQueryMethods()) {
                queries.put(
                        method,
                        lookupStrategy.resolveQuery(method,
                                repositoryInterface.getDomainClass()));
            }
        }


        /*
         * (non-Javadoc)
         * 
         * @see
         * org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance
         * .intercept.MethodInvocation)
         */
        public Object invoke(MethodInvocation invocation) throws Throwable {

            Method method = invocation.getMethod();

            if (isCustomMethodInvocation(invocation)) {

                makeAccessible(method);
                return executeMethodOn(customImplementation, method,
                        invocation.getArguments());
            }

            if (hasQueryFor(method)) {
                return queries.get(method).execute(invocation.getArguments());
            }

            // Lookup actual method as it might be redeclared in the interface
            // and we have to use the repository instance nevertheless
            Method actualMethod = metadata.getBaseClassMethod(method);
            return executeMethodOn(target, actualMethod,
                    invocation.getArguments());
        }


        /**
         * Executes the given method on the given target. Correctly unwraps
         * exceptions not caused by the reflection magic.
         * 
         * @param target
         * @param method
         * @param parameters
         * @return
         * @throws Throwable
         */
        private Object executeMethodOn(Object target, Method method,
                Object[] parameters) throws Throwable {

            try {
                return method.invoke(target, parameters);
            } catch (Exception e) {
                ClassUtils.unwrapReflectionException(e);
            }

            throw new IllegalStateException("Should not occur!");
        }


        /**
         * Returns whether we know of a query to execute for the given
         * {@link Method};
         * 
         * @param method
         * @return
         */
        private boolean hasQueryFor(Method method) {

            return queries.containsKey(method);
        }


        /**
         * Returns whether the given {@link MethodInvocation} is considered to
         * be targeted as an invocation of a custom method.
         * 
         * @param method
         * @return
         */
        private boolean isCustomMethodInvocation(MethodInvocation invocation) {

            if (null == customImplementation) {
                return false;
            }

            return metadata.isCustomMethod(invocation.getMethod());
        }
    }
}
