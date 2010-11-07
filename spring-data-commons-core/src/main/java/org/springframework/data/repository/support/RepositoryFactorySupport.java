/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.data.repository.support;

import static org.springframework.data.repository.util.ClassUtils.*;
import static org.springframework.util.ReflectionUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private QueryLookupStrategy.Key queryLookupStrategyKey;

    private final Map<Method, Method> methodCache =
            new ConcurrentHashMap<Method, Method>();
    private final List<RepositoryProxyPostProcessor> postProcessors =
            new ArrayList<RepositoryProxyPostProcessor>();


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
    protected void addDaoProxyPostProcessor(
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
     * @param customDaoImplementation
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Repository<?, ?>> T getRepository(
            Class<T> repositoryInterface, Object customDaoImplementation) {

        validate(repositoryInterface, customDaoImplementation);

        Class<?> domainClass = getDomainClass(repositoryInterface);
        RepositorySupport<?, ?> target =
                getTargetRepository(domainClass);

        // Create proxy
        ProxyFactory result = new ProxyFactory();
        result.setTarget(target);
        result.setInterfaces(new Class[] { repositoryInterface });

        for (RepositoryProxyPostProcessor processor : postProcessors) {
            processor.postProcess(result);
        }

        result.addAdvice(new QueryExecuterMethodInterceptor(
                repositoryInterface, customDaoImplementation, target));

        return (T) result.getProxy();
    }


    /**
     * Create a {@link RepositorySupport} instance as backing for the
     * query proxy.
     * 
     * @param <T>
     * @param domainClass
     * @return
     */
    protected abstract <T, ID extends Serializable> RepositorySupport<T, ID> getTargetRepository(
            Class<T> domainClass);


    /**
     * Create a {@link QueryMethod} instance for the given {@link Method}.
     * 
     * @param method
     * @return
     */
    protected abstract QueryMethod getQueryMethod(Method method);


    /**
     * Determines the base class for the repository to be created.
     * 
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected abstract Class<? extends RepositorySupport> getRepositoryClass();


    /**
     * Returns the {@link QueryLookupStrategy} for the given {@link Key}.
     * 
     * @param key can be {@literal null}
     * @return
     */
    protected abstract QueryLookupStrategy getQueryLookupStrategy(Key key);


    /**
     * Returns if the configured DAO interface has custom methods, that might
     * have to be delegated to a custom DAO implementation. This is used to
     * verify DAO configuration.
     * 
     * @return
     */
    private boolean hasCustomMethod(
            Class<? extends Repository<?, ?>> daoInterface) {

        boolean hasCustomMethod = false;

        // No detection required if no typing interface was configured
        if (isGenericRepositoryInterface(daoInterface)) {
            return false;
        }

        for (Method method : daoInterface.getMethods()) {

            if (isCustomMethod(method, daoInterface)
                    && !isBaseClassMethod(method, daoInterface)) {
                return true;
            }
        }

        return hasCustomMethod;
    }


    /**
     * Returns whether the given method is considered to be a DAO base class
     * method.
     * 
     * @param method
     * @return
     */
    private boolean isBaseClassMethod(Method method, Class<?> daoInterface) {

        Assert.notNull(method);

        if (method.getDeclaringClass().isAssignableFrom(getRepositoryClass())) {
            return true;
        }

        return !method.equals(getBaseClassMethod(method, daoInterface));
    }


    /**
     * Returns the base class method that is backing the given method. This can
     * be necessary if a DAO interface redeclares a method in {@link GenericDao}
     * (e.g. for transaction behaviour customization). Returns the method itself
     * if the base class does not implement the given method.
     * 
     * @param method
     * @return
     */
    private Method getBaseClassMethod(Method method, Class<?> daoInterface) {

        Assert.notNull(method);

        Method result = methodCache.get(method);

        if (null != result) {
            return result;
        }

        result =
                getBaseClassMethodFor(method, getRepositoryClass(),
                        daoInterface);
        methodCache.put(method, result);

        return result;
    }


    /**
     * Returns whether the given method is a custom DAO method.
     * 
     * @param method
     * @param daoInterface
     * @return
     */
    private boolean isCustomMethod(Method method, Class<?> daoInterface) {

        Class<?> declaringClass = method.getDeclaringClass();

        boolean isQueryMethod = declaringClass.equals(daoInterface);
        boolean isHadesDaoInterface =
                isGenericRepositoryInterface(declaringClass);
        boolean isBaseClassMethod = isBaseClassMethod(method, daoInterface);

        return !(isHadesDaoInterface || isBaseClassMethod || isQueryMethod);
    }


    /**
     * Returns all methods considered to be finder methods.
     * 
     * @param daoInterface
     * @return
     */
    private Iterable<Method> getFinderMethods(Class<?> daoInterface) {

        Set<Method> result = new HashSet<Method>();

        for (Method method : daoInterface.getDeclaredMethods()) {
            if (!isCustomMethod(method, daoInterface)
                    && !isBaseClassMethod(method, daoInterface)) {
                result.add(method);
            }
        }

        return result;
    }


    /**
     * Validates the given repository interface.
     * 
     * @param daoInterface
     */
    private void validate(Class<?> daoInterface) {

        Assert.notNull(daoInterface);
        Assert.notNull(
                getDomainClass(daoInterface),
                "Could not retrieve domain class from interface. Make sure it extends GenericRepository.");

    }


    /**
     * Validates the given repository interface as well as the given custom
     * implementation.
     * 
     * @param repositoryInterface
     * @param customImplementation
     */
    protected void validate(
            Class<? extends Repository<?, ?>> repositoryInterface,
            Object customImplementation) {

        validate(repositoryInterface);

        if (null == customImplementation
                && hasCustomMethod(repositoryInterface)) {

            throw new IllegalArgumentException(
                    String.format(
                            "You have custom methods in %s but not provided a custom implementation!",
                            repositoryInterface));
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
        private final Class<?> repositoryInterface;
        private final RepositorySupport<?, ?> target;


        /**
         * Creates a new {@link QueryExecuterMethodInterceptor}. Builds a model
         * of {@link QueryMethod}s to be invoked on execution of repository
         * interface methods.
         */
        public QueryExecuterMethodInterceptor(Class<?> repositoryInterface,
                Object customImplementation,
                RepositorySupport<?, ?> target) {

            this.repositoryInterface = repositoryInterface;
            this.customImplementation = customImplementation;
            this.target = target;

            QueryLookupStrategy strategy =
                    getQueryLookupStrategy(queryLookupStrategyKey);

            for (Method method : getFinderMethods(repositoryInterface)) {
                QueryMethod queryMethod = getQueryMethod(method);
                queries.put(method, strategy.resolveQuery(queryMethod));
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
            Method actualMethod =
                    getBaseClassMethod(method, repositoryInterface);
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

            return isCustomMethod(invocation.getMethod(), repositoryInterface);
        }
    }
}
