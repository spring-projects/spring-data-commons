/*
 * Copyright 2008-2017 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.DefaultEvaluationContextProvider;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.Pair;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.springframework.util.Assert;

/**
 * Factory bean to create instances of a given repository interface. Creates a proxy implementing the configured
 * repository interface and apply an advice handing the control to the {@code QueryExecuterMethodInterceptor}. Query
 * detection strategy can be configured by setting {@link QueryLookupStrategy.Key}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public abstract class RepositoryFactorySupport implements BeanClassLoaderAware, BeanFactoryAware {

	private final Map<RepositoryInformationCacheKey, RepositoryInformation> repositoryInformationCache;
	private final List<RepositoryProxyPostProcessor> postProcessors;

	private Optional<Class<?>> repositoryBaseClass;
	private QueryLookupStrategy.Key queryLookupStrategyKey;
	private List<QueryCreationListener<?>> queryPostProcessors;
	private NamedQueries namedQueries;
	private ClassLoader classLoader;
	private EvaluationContextProvider evaluationContextProvider;
	private BeanFactory beanFactory;

	private QueryCollectingQueryCreationListener collectingListener = new QueryCollectingQueryCreationListener();

	public RepositoryFactorySupport() {

		this.repositoryInformationCache = new HashMap<>();
		this.postProcessors = new ArrayList<>();

		this.repositoryBaseClass = Optional.empty();
		this.namedQueries = PropertiesBasedNamedQueries.EMPTY;
		this.classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();
		this.evaluationContextProvider = DefaultEvaluationContextProvider.INSTANCE;
		this.queryPostProcessors = new ArrayList<>();
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Sets the {@link EvaluationContextProvider} to be used to evaluate SpEL expressions in manually defined queries.
	 * 
	 * @param evaluationContextProvider can be {@literal null}, defaults to
	 *          {@link DefaultEvaluationContextProvider#INSTANCE}.
	 */
	public void setEvaluationContextProvider(EvaluationContextProvider evaluationContextProvider) {
		this.evaluationContextProvider = evaluationContextProvider == null ? DefaultEvaluationContextProvider.INSTANCE
				: evaluationContextProvider;
	}

	/**
	 * Configures the repository base class to use when creating the repository proxy. If not set, the factory will use
	 * the type returned by {@link #getRepositoryBaseClass(RepositoryMetadata)} by default.
	 * 
	 * @param repositoryBaseClass the repository base class to back the repository proxy, can be {@literal null}.
	 * @since 1.11
	 */
	public void setRepositoryBaseClass(Class<?> repositoryBaseClass) {
		this.repositoryBaseClass = Optional.ofNullable(repositoryBaseClass);
	}

	/**
	 * Adds a {@link QueryCreationListener} to the factory to plug in functionality triggered right after creation of
	 * {@link RepositoryQuery} instances.
	 * 
	 * @param listener
	 */
	public void addQueryCreationListener(QueryCreationListener<?> listener) {

		Assert.notNull(listener, "Listener must not be null!");
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

		Assert.notNull(processor, "RepositoryProxyPostProcessor must not be null!");
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
		return getRepository(repositoryInterface, Optional.empty());
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
	public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {
		return getRepository(repositoryInterface, Optional.of(customImplementation));
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
	protected <T> T getRepository(Class<T> repositoryInterface, Optional<Object> customImplementation) {

		RepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
		RepositoryInformation information = getRepositoryInformation(metadata, customImplementation.map(Object::getClass));

		validate(information, customImplementation);

		Object target = getTargetRepository(information);

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(new Class[] { repositoryInterface, Repository.class, TransactionalProxy.class });

		result.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
		result.addAdvisor(ExposeInvocationInterceptor.ADVISOR);

		postProcessors.forEach(processor -> processor.postProcess(result, information));

		result.addAdvice(new DefaultMethodInvokingMethodInterceptor());
		result.addAdvice(new QueryExecutorMethodInterceptor(information));

		result.addAdvice(information.isReactiveRepository()
				? new ConvertingImplementationMethodExecutionInterceptor(information, customImplementation, target)
				: new ImplementationMethodExecutionInterceptor(information, customImplementation, target));

		return (T) result.getProxy(classLoader);
	}

	/**
	 * Returns the {@link RepositoryMetadata} for the given repository interface.
	 * 
	 * @param repositoryInterface will never be {@literal null}.
	 * @return
	 */
	protected RepositoryMetadata getRepositoryMetadata(Class<?> repositoryInterface) {
		return AbstractRepositoryMetadata.getMetadata(repositoryInterface);
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given repository interface.
	 * 
	 * @param metadata
	 * @param customImplementationClass
	 * @return
	 */
	protected RepositoryInformation getRepositoryInformation(RepositoryMetadata metadata,
			Optional<Class<?>> customImplementationClass) {

		RepositoryInformationCacheKey cacheKey = new RepositoryInformationCacheKey(metadata, customImplementationClass);

		return repositoryInformationCache.computeIfAbsent(cacheKey, key -> {

			Class<?> baseClass = repositoryBaseClass.orElse(getRepositoryBaseClass(metadata));

			return metadata.isReactiveRepository()
					? new ReactiveRepositoryInformation(metadata, baseClass, customImplementationClass)
					: new DefaultRepositoryInformation(metadata, baseClass, customImplementationClass);
		});
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
	protected abstract Object getTargetRepository(RepositoryInformation metadata);

	/**
	 * Returns the base class backing the actual repository instance. Make sure
	 * {@link #getTargetRepository(RepositoryMetadata)} returns an instance of this class.
	 * 
	 * @param metadata
	 * @return
	 */
	protected abstract Class<?> getRepositoryBaseClass(RepositoryMetadata metadata);

	/**
	 * Returns the {@link QueryLookupStrategy} for the given {@link Key} and {@link EvaluationContextProvider}.
	 * 
	 * @param key can be {@literal null}.
	 * @param evaluationContextProvider will never be {@literal null}.
	 * @return the {@link QueryLookupStrategy} to use or {@literal null} if no queries should be looked up.
	 * @since 1.9
	 */
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			EvaluationContextProvider evaluationContextProvider) {
		return Optional.empty();
	}

	/**
	 * Validates the given repository interface as well as the given custom implementation.
	 * 
	 * @param repositoryInformation
	 * @param customImplementation
	 */
	private void validate(RepositoryInformation repositoryInformation, Optional<Object> customImplementation) {

		customImplementation.orElseGet(() -> {

			if (!repositoryInformation.hasCustomMethod()) {
				return null;
			}

			throw new IllegalArgumentException(
					String.format("You have custom methods in %s but not provided a custom implementation!",
							repositoryInformation.getRepositoryInterface()));
		});

		validate(repositoryInformation);
	}

	protected void validate(RepositoryMetadata repositoryMetadata) {

	}

	/**
	 * Creates a repository of the repository base class defined in the given {@link RepositoryInformation} using
	 * reflection.
	 * 
	 * @param information
	 * @param constructorArguments
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected final <R> R getTargetRepositoryViaReflection(RepositoryInformation information,
			Object... constructorArguments) {

		Class<?> baseClass = information.getRepositoryBaseClass();
		Optional<Constructor<?>> constructor = ReflectionUtils.findConstructor(baseClass, constructorArguments);

		return constructor.map(it -> (R) BeanUtils.instantiateClass(it, constructorArguments))
				.orElseThrow(() -> new IllegalStateException(String.format(
						"No suitable constructor found on %s to match the given arguments: %s. Make sure you implement a constructor taking these",
						baseClass, Arrays.stream(constructorArguments).map(Object::getClass).collect(Collectors.toList()))));
	}

	/**
	 * This {@code MethodInterceptor} intercepts calls to methods of the custom implementation and delegates the to it if
	 * configured. Furthermore it resolves method calls to finders and triggers execution of them. You can rely on having
	 * a custom repository implementation instance set if this returns true.
	 * 
	 * @author Oliver Gierke
	 */
	public class QueryExecutorMethodInterceptor implements MethodInterceptor {

		private final Map<Method, RepositoryQuery> queries;
		private final QueryExecutionResultHandler resultHandler;

		/**
		 * Creates a new {@link QueryExecutorMethodInterceptor}. Builds a model of {@link QueryMethod}s to be invoked on
		 * execution of repository interface methods.
		 */
		public QueryExecutorMethodInterceptor(RepositoryInformation repositoryInformation) {

			this.resultHandler = new QueryExecutionResultHandler();

			Optional<QueryLookupStrategy> lookupStrategy = getQueryLookupStrategy(queryLookupStrategyKey,
					RepositoryFactorySupport.this.evaluationContextProvider);

			if (!lookupStrategy.isPresent() && repositoryInformation.hasQueryMethods()) {

				throw new IllegalStateException("You have defined query method in the repository but "
						+ "you don't have any query lookup strategy defined. The "
						+ "infrastructure apparently does not support query methods!");
			}

			this.queries = lookupStrategy.map(it -> {

				SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory();
				factory.setBeanClassLoader(classLoader);
				factory.setBeanFactory(beanFactory);

				return repositoryInformation.getQueryMethods().stream()//
						.map(method -> Pair.of(method, it.resolveQuery(method, repositoryInformation, factory, namedQueries)))//
						.peek(pair -> invokeListeners(pair.getSecond()))//
						.collect(Pair.toMap());

			}).orElse(Collections.emptyMap());
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void invokeListeners(RepositoryQuery query) {

			for (QueryCreationListener listener : queryPostProcessors) {

				ResolvableType typeArgument = ResolvableType.forClass(QueryCreationListener.class, listener.getClass())
						.getGeneric(0);

				if (typeArgument != null && typeArgument.isAssignableFrom(ResolvableType.forClass(query.getClass()))) {
					listener.onCreation(query);
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Object result = doInvoke(invocation);

			// Looking up the TypeDescriptor for the return type - yes, this way o.O
			Method method = invocation.getMethod();
			MethodParameter parameter = new MethodParameter(method, -1);
			TypeDescriptor methodReturnTypeDescriptor = TypeDescriptor.nested(parameter, 0);

			return resultHandler.postProcessInvocationResult(result, methodReturnTypeDescriptor);
		}

		private Object doInvoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();

			if (hasQueryFor(method)) {
				return queries.get(method).execute(arguments);
			}

			return invocation.proceed();
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
	}

	/**
	 * Method interceptor that calls methods on either the base implementation or the custom repository implementation.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	public class ImplementationMethodExecutionInterceptor implements MethodInterceptor {

		private final RepositoryInformation repositoryInformation;
		private final Optional<Object> customImplementation;
		private final Object target;

		/* (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();

			if (isCustomMethodInvocation(invocation)) {

				Method actualMethod = repositoryInformation.getTargetClassMethod(method);
				return executeMethodOn(customImplementation.get(), actualMethod, arguments);
			}

			// Lookup actual method as it might be redeclared in the interface
			// and we have to use the repository instance nevertheless
			Method actualMethod = repositoryInformation.getTargetClassMethod(method);
			return executeMethodOn(target, actualMethod, arguments);
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
		protected Object executeMethodOn(Object target, Method method, Object[] parameters) throws Throwable {

			try {
				return method.invoke(target, parameters);
			} catch (Exception e) {
				ClassUtils.unwrapReflectionException(e);
			}

			throw new IllegalStateException("Should not occur!");
		}

		/**
		 * Returns whether the given {@link MethodInvocation} is considered to be targeted as an invocation of a custom
		 * method.
		 * 
		 * @param method
		 * @return
		 */
		private boolean isCustomMethodInvocation(MethodInvocation invocation) {
			return customImplementation.map(it -> repositoryInformation.isCustomMethod(invocation.getMethod())).orElse(false);
		}
	}

	/**
	 * Method interceptor that converts parameters before invoking a method.
	 *
	 * @author Mark Paluch
	 */
	public class ConvertingImplementationMethodExecutionInterceptor extends ImplementationMethodExecutionInterceptor {

		/**
		 * @param repositoryInformation
		 * @param customImplementation
		 * @param target
		 */
		public ConvertingImplementationMethodExecutionInterceptor(RepositoryInformation repositoryInformation,
				Optional<Object> customImplementation, Object target) {

			super(repositoryInformation, customImplementation, target);
		}

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport.ImplementationMethodExecutionInterceptor#executeMethodOn(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		protected Object executeMethodOn(Object target, Method method, Object[] parameters) throws Throwable {
			return super.executeMethodOn(target, method, convertParameters(method.getParameterTypes(), parameters));
		}

		/**
		 * @param parameterTypes
		 * @param parameters
		 * @return
		 */
		private Object[] convertParameters(Class<?>[] parameterTypes, Object[] parameters) {

			if (parameters.length == 0) {
				return parameters;
			}

			Object[] result = new Object[parameters.length];

			for (int i = 0; i < parameters.length; i++) {

				if (parameters[i] == null) {
					continue;
				}

				if (!parameterTypes[i].isAssignableFrom(parameters[i].getClass()) && ReactiveWrappers.isAvailable()
						&& ReactiveWrapperConverters.canConvert(parameters[i].getClass(), parameterTypes[i])) {

					result[i] = ReactiveWrapperConverters.toWrapper(parameters[i], parameterTypes[i]);
				} else {
					result[i] = parameters[i];
				}

			}

			return result;
		}
	}

	/**
	 * {@link QueryCreationListener} collecting the {@link QueryMethod}s created for all query methods of the repository
	 * interface.
	 * 
	 * @author Oliver Gierke
	 */
	@Getter
	private static class QueryCollectingQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

		/**
		 * All {@link QueryMethod}s.
		 */
		private List<QueryMethod> queryMethods = new ArrayList<>();

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.QueryCreationListener#onCreation(org.springframework.data.repository.query.RepositoryQuery)
		 */
		public void onCreation(RepositoryQuery query) {
			this.queryMethods.add(query.getQueryMethod());
		}
	}

	/**
	 * Simple value object to build up keys to cache {@link RepositoryInformation} instances.
	 * 
	 * @author Oliver Gierke
	 */
	@EqualsAndHashCode
	private static class RepositoryInformationCacheKey {

		private final String repositoryInterfaceName;
		private final String customImplementationClassName;

		/**
		 * Creates a new {@link RepositoryInformationCacheKey} for the given {@link RepositoryMetadata} and cuytom
		 * implementation type.
		 * 
		 * @param repositoryInterfaceName must not be {@literal null}.
		 * @param customImplementationClassName
		 */
		public RepositoryInformationCacheKey(RepositoryMetadata metadata, Optional<Class<?>> customImplementationType) {

			this.repositoryInterfaceName = metadata.getRepositoryInterface().getName();
			this.customImplementationClassName = customImplementationType.map(Class::getName).orElse(null);
		}
	}
}
