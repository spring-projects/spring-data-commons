/*
 * Copyright 2008-2018 the original author or authors.
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
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
import org.springframework.core.ResolvableType;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
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
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Factory bean to create instances of a given repository interface. Creates a proxy implementing the configured
 * repository interface and apply an advice handing the control to the {@code QueryExecuterMethodInterceptor}. Query
 * detection strategy can be configured by setting {@link QueryLookupStrategy.Key}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public abstract class RepositoryFactorySupport implements BeanClassLoaderAware, BeanFactoryAware {

	private static final BiFunction<Method, Object[], Object[]> REACTIVE_ARGS_CONVERTER = (method, o) -> {

		if (ReactiveWrappers.isAvailable()) {

			Class<?>[] parameterTypes = method.getParameterTypes();

			Object[] converted = new Object[o.length];
			for (int i = 0; i < parameterTypes.length; i++) {

				Class<?> parameterType = parameterTypes[i];
				Object value = o[i];

				if (value == null) {
					continue;
				}

				if (!parameterType.isAssignableFrom(value.getClass())
						&& ReactiveWrapperConverters.canConvert(value.getClass(), parameterType)) {

					converted[i] = ReactiveWrapperConverters.toWrapper(value, parameterType);
				} else {
					converted[i] = value;
				}
			}

			return converted;
		}

		return o;
	};

	private final Map<RepositoryInformationCacheKey, RepositoryInformation> repositoryInformationCache;
	private final List<RepositoryProxyPostProcessor> postProcessors;

	private Optional<Class<?>> repositoryBaseClass;
	private @Nullable QueryLookupStrategy.Key queryLookupStrategyKey;
	private List<QueryCreationListener<?>> queryPostProcessors;
	private NamedQueries namedQueries;
	private ClassLoader classLoader;
	private EvaluationContextProvider evaluationContextProvider;
	private BeanFactory beanFactory;

	private final QueryCollectingQueryCreationListener collectingListener = new QueryCollectingQueryCreationListener();

	@SuppressWarnings("null")
	public RepositoryFactorySupport() {

		this.repositoryInformationCache = new ConcurrentReferenceHashMap<>(16, ReferenceType.WEAK);
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
	 * Creates {@link RepositoryFragments} based on {@link RepositoryMetadata} to add repository-specific extensions.
	 *
	 * @param metadata
	 * @return
	 */
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
		return RepositoryFragments.empty();
	}

	/**
	 * Creates {@link RepositoryComposition} based on {@link RepositoryMetadata} for repository-specific method handling.
	 *
	 * @param metadata
	 * @return
	 */
	private RepositoryComposition getRepositoryComposition(RepositoryMetadata metadata) {

		RepositoryComposition composition = RepositoryComposition.empty();

		if (metadata.isReactiveRepository()) {
			return composition.withMethodLookup(MethodLookups.forReactiveTypes(metadata))
					.withArgumentConverter(REACTIVE_ARGS_CONVERTER);
		}

		return composition.withMethodLookup(MethodLookups.forRepositoryTypes(metadata));
	}

	/**
	 * Returns a repository instance for the given interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @return
	 */
	public <T> T getRepository(Class<T> repositoryInterface) {
		return getRepository(repositoryInterface, RepositoryFragments.empty());
	}

	/**
	 * Returns a repository instance for the given interface backed by an instance providing implementation logic for
	 * custom logic.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @param customImplementation must not be {@literal null}.
	 * @return
	 * @deprecated since 2.0. Use {@link RepositoryFragments} with {@link #getRepository(Class, RepositoryFragments)} to
	 *             compose repositories backed by custom implementations.
	 */
	@Deprecated
	public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {
		return getRepository(repositoryInterface, RepositoryFragments.just(customImplementation));
	}

	/**
	 * Returns a repository instance for the given interface backed by an instance providing implementation logic for
	 * custom logic.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @param fragments must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	@SuppressWarnings({ "unchecked" })
	public <T> T getRepository(Class<T> repositoryInterface, RepositoryFragments fragments) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null!");
		Assert.notNull(fragments, "RepositoryFragments must not be null!");

		RepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
		RepositoryComposition composition = getRepositoryComposition(metadata, fragments);
		RepositoryInformation information = getRepositoryInformation(metadata, composition);

		validate(information, composition);

		Object target = getTargetRepository(information);

		// Create proxy
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(repositoryInterface, Repository.class, TransactionalProxy.class);

		if (MethodInvocationValidator.supports(repositoryInterface)) {
			result.addAdvice(new MethodInvocationValidator());
		}

		result.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
		result.addAdvisor(ExposeInvocationInterceptor.ADVISOR);

		postProcessors.forEach(processor -> processor.postProcess(result, information));

		result.addAdvice(new DefaultMethodInvokingMethodInterceptor());

		ProjectionFactory projectionFactory = getProjectionFactory(classLoader, beanFactory);
		result.addAdvice(new QueryExecutorMethodInterceptor(information, projectionFactory));

		composition = composition.append(RepositoryFragment.implemented(target));
		result.addAdvice(new ImplementationMethodExecutionInterceptor(composition));

		return (T) result.getProxy(classLoader);
	}

	/**
	 * Returns the {@link ProjectionFactory} to be used with the repository instances created.
	 *
	 * @param classLoader will never be {@literal null}.
	 * @param beanFactory will never be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	protected ProjectionFactory getProjectionFactory(ClassLoader classLoader, BeanFactory beanFactory) {

		SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		factory.setBeanClassLoader(classLoader);
		factory.setBeanFactory(beanFactory);

		return factory;
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
	 * Returns the {@link RepositoryInformation} for the given {@link RepositoryMetadata} and custom
	 * {@link RepositoryFragments}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param fragments must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	protected RepositoryInformation getRepositoryInformation(RepositoryMetadata metadata, RepositoryFragments fragments) {
		return getRepositoryInformation(metadata, getRepositoryComposition(metadata, fragments));
	}

	/**
	 * Returns the {@link RepositoryComposition} for the given {@link RepositoryMetadata} and extra
	 * {@link RepositoryFragments}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param fragments must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private RepositoryComposition getRepositoryComposition(RepositoryMetadata metadata, RepositoryFragments fragments) {

		Assert.notNull(metadata, "RepositoryMetadata must not be null!");
		Assert.notNull(fragments, "RepositoryFragments must not be null!");

		RepositoryComposition composition = getRepositoryComposition(metadata);
		RepositoryFragments repositoryAspects = getRepositoryFragments(metadata);

		return composition.append(fragments).append(repositoryAspects);
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given repository interface.
	 *
	 * @param metadata
	 * @param composition
	 * @return
	 */
	private RepositoryInformation getRepositoryInformation(RepositoryMetadata metadata,
			RepositoryComposition composition) {

		RepositoryInformationCacheKey cacheKey = new RepositoryInformationCacheKey(metadata, composition);

		return repositoryInformationCache.computeIfAbsent(cacheKey, key -> {

			Class<?> baseClass = repositoryBaseClass.orElse(getRepositoryBaseClass(metadata));

			return new DefaultRepositoryInformation(metadata, baseClass, composition);
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
	public abstract <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass);

	/**
	 * Create a repository instance as backing for the query proxy.
	 *
	 * @param metadata
	 * @return
	 */
	protected abstract Object getTargetRepository(RepositoryInformation metadata);

	/**
	 * Returns the base class backing the actual repository instance. Make sure
	 * {@link #getTargetRepository(RepositoryInformation)} returns an instance of this class.
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
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			EvaluationContextProvider evaluationContextProvider) {
		return Optional.empty();
	}

	/**
	 * Validates the given repository interface as well as the given custom implementation.
	 *
	 * @param repositoryInformation
	 * @param composition
	 */
	private void validate(RepositoryInformation repositoryInformation, RepositoryComposition composition) {

		if (repositoryInformation.hasCustomMethod()) {

			if (composition.isEmpty()) {

				throw new IllegalArgumentException(
						String.format("You have custom methods in %s but not provided a custom implementation!",
								repositoryInformation.getRepositoryInterface()));
			}

			composition.validateImplementation();
		}

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
	protected final <R> R getTargetRepositoryViaReflection(RepositoryInformation information,
			Object... constructorArguments) {

		Class<?> baseClass = information.getRepositoryBaseClass();
		return getTargetRepositoryViaReflection(baseClass, constructorArguments);
	}

	/**
	 * Creates a repository of the repository base class defined in the given {@link RepositoryInformation} using
	 * reflection.
	 *
	 * @param baseClass
	 * @param constructorArguments
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected final <R> R getTargetRepositoryViaReflection(Class<?> baseClass, Object... constructorArguments) {
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
		public QueryExecutorMethodInterceptor(RepositoryInformation repositoryInformation,
				ProjectionFactory projectionFactory) {

			this.resultHandler = new QueryExecutionResultHandler();

			Optional<QueryLookupStrategy> lookupStrategy = getQueryLookupStrategy(queryLookupStrategyKey,
					RepositoryFactorySupport.this.evaluationContextProvider);

			if (!lookupStrategy.isPresent() && repositoryInformation.hasQueryMethods()) {

				throw new IllegalStateException("You have defined query method in the repository but "
						+ "you don't have any query lookup strategy defined. The "
						+ "infrastructure apparently does not support query methods!");
			}

			this.queries = lookupStrategy //
					.map(it -> mapMethodsToQuery(repositoryInformation, it, projectionFactory)) //
					.orElse(Collections.emptyMap());
		}

		private Map<Method, RepositoryQuery> mapMethodsToQuery(RepositoryInformation repositoryInformation,
				QueryLookupStrategy lookupStrategy, ProjectionFactory projectionFactory) {

			return repositoryInformation.getQueryMethods().stream() //
					.map(method -> lookupQuery(method, repositoryInformation, lookupStrategy, projectionFactory)) //
					.peek(pair -> invokeListeners(pair.getSecond())) //
					.collect(Pair.toMap());
		}

		private Pair<Method, RepositoryQuery> lookupQuery(Method method, RepositoryInformation information,
				QueryLookupStrategy strategy, ProjectionFactory projectionFactory) {
			return Pair.of(method, strategy.resolveQuery(method, information, projectionFactory, namedQueries));
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
		@Override
		@Nullable
		public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

			Object result = doInvoke(invocation);

			return resultHandler.postProcessInvocationResult(result, invocation.getMethod());
		}

		@Nullable
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
	 * Method interceptor that calls methods on the {@link RepositoryComposition}.
	 *
	 * @author Mark Paluch
	 */
	@RequiredArgsConstructor
	public class ImplementationMethodExecutionInterceptor implements MethodInterceptor {

		private final @NonNull RepositoryComposition composition;

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Nullable
		@Override
		public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();

			try {
				return composition.invoke(method, arguments);
			} catch (Exception e) {
				ClassUtils.unwrapReflectionException(e);
			}

			throw new IllegalStateException("Should not occur!");
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
		private final List<QueryMethod> queryMethods = new ArrayList<>();

		/* (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.QueryCreationListener#onCreation(org.springframework.data.repository.query.RepositoryQuery)
		 */
		@Override
		public void onCreation(RepositoryQuery query) {
			this.queryMethods.add(query.getQueryMethod());
		}
	}

	/**
	 * Simple value object to build up keys to cache {@link RepositoryInformation} instances.
	 *
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	@EqualsAndHashCode
	@Value
	private static class RepositoryInformationCacheKey {

		String repositoryInterfaceName;
		final long compositionHash;

		/**
		 * Creates a new {@link RepositoryInformationCacheKey} for the given {@link RepositoryMetadata} and composition.
		 *
		 * @param metadata must not be {@literal null}.
		 * @param composition must not be {@literal null}.
		 */
		public RepositoryInformationCacheKey(RepositoryMetadata metadata, RepositoryComposition composition) {

			this.repositoryInterfaceName = metadata.getRepositoryInterface().getName();
			this.compositionHash = composition.hashCode();
		}
	}
}
