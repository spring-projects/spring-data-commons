/*
 * Copyright 2008-2013 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.springframework.util.ReflectionUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;

/**
 * Factory bean to create instances of a given repository interface. Creates a proxy implementing the configured
 * repository interface and apply an advice handing the control to the {@code QueryExecuterMethodInterceptor}. Query
 * detection strategy can be configured by setting {@link QueryLookupStrategy.Key}.
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryFactorySupport implements BeanClassLoaderAware {

	private final List<RepositoryProxyPostProcessor> postProcessors = new ArrayList<RepositoryProxyPostProcessor>();
	private QueryLookupStrategy.Key queryLookupStrategyKey;
	private List<QueryCreationListener<?>> queryPostProcessors = new ArrayList<QueryCreationListener<?>>();
	private NamedQueries namedQueries = PropertiesBasedNamedQueries.EMPTY;
	private ClassLoader classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();

	private QueryCollectingQueryCreationListener collectingListener = new QueryCollectingQueryCreationListener();

	public RepositoryFactorySupport() {
		this.queryPostProcessors.add(collectingListener);
	}

	/**
	 * Sets the strategy of how to lookup a query to execute finders.
	 * 
	 * @param key
	 */
	public void setQueryLookupStrategyKey(Key key) {
		this.queryLookupStrategyKey = key;
	}

	/**
	 * Configures a {@link NamedQueries} instance to be handed to the {@link QueryLookupStrategy} for query creation.
	 * 
	 * @param namedQueries the namedQueries to set
	 */
	public void setNamedQueries(NamedQueries namedQueries) {
		this.namedQueries = namedQueries == null ? PropertiesBasedNamedQueries.EMPTY : namedQueries;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader == null ? org.springframework.util.ClassUtils.getDefaultClassLoader() : classLoader;
	}

	/**
	 * Adds a {@link QueryCreationListener} to the factory to plug in functionality triggered right after creation of
	 * {@link RepositoryQuery} instances.
	 * 
	 * @param listener
	 */
	public void addQueryCreationListener(QueryCreationListener<?> listener) {

		Assert.notNull(listener);
		this.queryPostProcessors.add(listener);
	}

	/**
	 * Adds {@link RepositoryProxyPostProcessor}s to the factory to allow manipulation of the {@link ProxyFactory} before
	 * the proxy gets created. Note that the {@link QueryExecutorMethodInterceptor} will be added to the proxy
	 * <em>after</em> the {@link RepositoryProxyPostProcessor}s are considered.
	 * 
	 * @param processor
	 */
	public void addRepositoryProxyPostProcessor(RepositoryProxyPostProcessor processor) {

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
	public <T> T getRepository(Class<T> repositoryInterface) {

		return getRepository(repositoryInterface, null);
	}

	/**
	 * Returns a repository instance for the given interface backed by an instance providing implementation logic for
	 * custom logic.
	 * 
	 * @param <T>
	 * @param repositoryInterface
	 * @param customImplementation
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {

		RepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
		Class<?> customImplementationClass = null == customImplementation ? null : customImplementation.getClass();
		RepositoryInformation information = getRepositoryInformation(metadata, customImplementationClass);

		validate(information, customImplementation);

		Object target = getTargetRepository(information);

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(new Class[] { repositoryInterface, Repository.class });

		for (RepositoryProxyPostProcessor processor : postProcessors) {
			processor.postProcess(result);
		}

		result.addAdvice(new QueryExecutorMethodInterceptor(information, customImplementation, target));

		return (T) result.getProxy(classLoader);
	}

	/**
	 * Returns the {@link RepositoryMetadata} for the given repository interface.
	 * 
	 * @param repositoryInterface
	 * @return
	 */
	RepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
		return Repository.class.isAssignableFrom(repositoryInterface) ? new DefaultRepositoryMetadata(repositoryInterface)
				: new AnnotationRepositoryMetadata(repositoryInterface);
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given repository interface.
	 * 
	 * @param metadata
	 * @param customImplementationClass
	 * @return
	 */
	protected RepositoryInformation getRepositoryInformation(RepositoryMetadata metadata,
			Class<?> customImplementationClass) {
		return new DefaultRepositoryInformation(metadata, getRepositoryBaseClass(metadata), customImplementationClass);
	}

	protected List<QueryMethod> getQueryMethods() {
		return collectingListener.getQueryMethods();
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param <T> the entity type
	 * @param <ID> the id type
	 * @param domainClass
	 * @return
	 */
	public abstract <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass);

	/**
	 * Create a repository instance as backing for the query proxy.
	 * 
	 * @param metadata
	 * @return
	 */
	protected abstract Object getTargetRepository(RepositoryMetadata metadata);

	/**
	 * Returns the base class backing the actual repository instance. Make sure
	 * {@link #getTargetRepository(RepositoryMetadata)} returns an instance of this class.
	 * 
	 * @param metadata
	 * @return
	 */
	protected abstract Class<?> getRepositoryBaseClass(RepositoryMetadata metadata);

	/**
	 * Returns the {@link QueryLookupStrategy} for the given {@link Key}.
	 * 
	 * @param key can be {@literal null}
	 * @return the {@link QueryLookupStrategy} to use or {@literal null} if no queries should be looked up.
	 */
	protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
		return null;
	}

	/**
	 * Validates the given repository interface as well as the given custom implementation.
	 * 
	 * @param repositoryInformation
	 * @param customImplementation
	 */
	private void validate(RepositoryInformation repositoryInformation, Object customImplementation) {

		if (null == customImplementation && repositoryInformation.hasCustomMethod()) {

			throw new IllegalArgumentException(String.format(
					"You have custom methods in %s but not provided a custom implementation!",
					repositoryInformation.getRepositoryInterface()));
		}

		validate(repositoryInformation);
	}

	protected void validate(RepositoryMetadata repositoryMetadata) {

	}

	/**
	 * This {@code MethodInterceptor} intercepts calls to methods of the custom implementation and delegates the to it if
	 * configured. Furthermore it resolves method calls to finders and triggers execution of them. You can rely on having
	 * a custom repository implementation instance set if this returns true.
	 * 
	 * @author Oliver Gierke
	 */
	public class QueryExecutorMethodInterceptor implements MethodInterceptor {

		private final Map<Method, RepositoryQuery> queries = new ConcurrentHashMap<Method, RepositoryQuery>();

		private final Object customImplementation;
		private final RepositoryInformation repositoryInformation;
		private final Object target;

		/**
		 * Creates a new {@link QueryExecutorMethodInterceptor}. Builds a model of {@link QueryMethod}s to be invoked on
		 * execution of repository interface methods.
		 */
		public QueryExecutorMethodInterceptor(RepositoryInformation repositoryInformation, Object customImplementation,
				Object target) {

			this.repositoryInformation = repositoryInformation;
			this.customImplementation = customImplementation;
			this.target = target;

			QueryLookupStrategy lookupStrategy = getQueryLookupStrategy(queryLookupStrategyKey);
			Iterable<Method> queryMethods = repositoryInformation.getQueryMethods();

			if (lookupStrategy == null) {

				if (queryMethods.iterator().hasNext()) {
					throw new IllegalStateException("You have defined query method in the repository but "
							+ "you don't have no query lookup strategy defined. The "
							+ "infrastructure apparently does not support query methods!");
				}

				return;
			}

			for (Method method : queryMethods) {
				RepositoryQuery query = lookupStrategy.resolveQuery(method, repositoryInformation, namedQueries);
				invokeListeners(query);
				queries.put(method, query);
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void invokeListeners(RepositoryQuery query) {

			for (QueryCreationListener listener : queryPostProcessors) {
				Class<?> typeArgument = GenericTypeResolver.resolveTypeArgument(listener.getClass(),
						QueryCreationListener.class);
				if (typeArgument != null && typeArgument.isAssignableFrom(query.getClass())) {
					listener.onCreation(query);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();

			if (isCustomMethodInvocation(invocation)) {
				Method actualMethod = repositoryInformation.getTargetClassMethod(method);
				makeAccessible(actualMethod);
				return executeMethodOn(customImplementation, actualMethod, invocation.getArguments());
			}

			if (hasQueryFor(method)) {
				return queries.get(method).execute(invocation.getArguments());
			}

			// Lookup actual method as it might be redeclared in the interface
			// and we have to use the repository instance nevertheless
			Method actualMethod = repositoryInformation.getTargetClassMethod(method);
			return executeMethodOn(target, actualMethod, invocation.getArguments());
		}

		/**
		 * Executes the given method on the given target. Correctly unwraps exceptions not caused by the reflection magic.
		 * 
		 * @param target
		 * @param method
		 * @param parameters
		 * @return
		 * @throws Throwable
		 */
		private Object executeMethodOn(Object target, Method method, Object[] parameters) throws Throwable {

			try {
				return method.invoke(target, parameters);
			} catch (Exception e) {
				ClassUtils.unwrapReflectionException(e);
			}

			throw new IllegalStateException("Should not occur!");
		}

		/**
		 * Returns whether we know of a query to execute for the given {@link Method};
		 * 
		 * @param method
		 * @return
		 */
		private boolean hasQueryFor(Method method) {

			return queries.containsKey(method);
		}

		/**
		 * Returns whether the given {@link MethodInvocation} is considered to be targeted as an invocation of a custom
		 * method.
		 * 
		 * @param method
		 * @return
		 */
		private boolean isCustomMethodInvocation(MethodInvocation invocation) {

			if (null == customImplementation) {
				return false;
			}

			return repositoryInformation.isCustomMethod(invocation.getMethod());
		}
	}

	/**
	 * {@link QueryCreationListener} collecting the {@link QueryMethod}s created for all query methods of the repository
	 * interface.
	 * 
	 * @author Oliver Gierke
	 */
	private static class QueryCollectingQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

		private List<QueryMethod> queryMethods = new ArrayList<QueryMethod>();

		/**
		 * Returns all {@link QueryMethod}s.
		 * 
		 * @return
		 */
		public List<QueryMethod> getQueryMethods() {
			return queryMethods;
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.QueryCreationListener#onCreation(org.springframework.data.repository.query.RepositoryQuery)
		 */
		public void onCreation(RepositoryQuery query) {
			this.queryMethods.add(query.getQueryMethod());
		}
	}
}
