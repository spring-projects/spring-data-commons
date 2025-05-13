/*
 * Copyright 2008-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.log.LogMessage;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.projection.DefaultMethodInvokingMethodInterceptor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.data.repository.core.RepositoryMethodContextHolder;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryInvocationMulticaster.DefaultRepositoryInvocationMulticaster;
import org.springframework.data.repository.core.support.RepositoryInvocationMulticaster.NoOpRepositoryInvocationMulticaster;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodValueEvaluationContextAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.NullnessMethodInvocationValidator;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.transaction.interceptor.TransactionalProxy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Factory bean to create instances of a given repository interface. Creates a proxy implementing the configured
 * repository interface and apply an advice handing the control to the {@code QueryExecutorMethodInterceptor}. Query
 * detection strategy can be configured by setting {@link QueryLookupStrategy.Key}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author John Blum
 * @author Johannes Englmeier
 */
public abstract class RepositoryFactorySupport
		implements BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware, EnvironmentCapable {

	static final GenericConversionService CONVERSION_SERVICE = new DefaultConversionService();
	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
	private static final ValueExpressionParser VALUE_PARSER = ValueExpressionParser.create(() -> EXPRESSION_PARSER);

	private static final Log logger = LogFactory.getLog(RepositoryFactorySupport.class);

	static {
		QueryExecutionConverters.registerConvertersIn(CONVERSION_SERVICE);
		CONVERSION_SERVICE.removeConvertible(Object.class, Object.class);
	}

	private final Map<RepositoryInformationCacheKey, RepositoryStub> repositoryInformationCache;
	private final List<RepositoryProxyPostProcessor> postProcessors;

	private @Nullable Class<?> repositoryBaseClass;
	private boolean exposeMetadata;
	private QueryLookupStrategy.@Nullable Key queryLookupStrategyKey;
	private final List<QueryCreationListener<?>> queryPostProcessors;
	private final List<RepositoryMethodInvocationListener> methodInvocationListeners;
	private NamedQueries namedQueries;
	private @Nullable ClassLoader classLoader;
	private EvaluationContextProvider evaluationContextProvider;
	private @Nullable BeanFactory beanFactory;
	private @Nullable Environment environment;
	private Lazy<ProjectionFactory> projectionFactory;

	private final QueryCollectingQueryCreationListener collectingListener = new QueryCollectingQueryCreationListener();

	public RepositoryFactorySupport() {

		this.repositoryInformationCache = new HashMap<>(8);
		this.postProcessors = new ArrayList<>();

		this.namedQueries = PropertiesBasedNamedQueries.EMPTY;
		this.classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();
		this.evaluationContextProvider = QueryMethodValueEvaluationContextAccessor.DEFAULT_CONTEXT_PROVIDER;
		this.queryPostProcessors = new ArrayList<>();
		this.queryPostProcessors.add(collectingListener);
		this.methodInvocationListeners = new ArrayList<>();
		this.projectionFactory = createProjectionFactory();
	}

	EvaluationContextProvider getEvaluationContextProvider() {
		return evaluationContextProvider;
	}

	/**
	 * Set whether the repository method metadata should be exposed by the repository factory as a ThreadLocal for
	 * retrieval via the {@code RepositoryMethodContext} class. This is useful if an advised object needs to obtain
	 * repository information.
	 * <p>
	 * Default is {@literal "false"}, in order to avoid unnecessary extra interception. This means that no guarantees are
	 * provided that {@code RepositoryMethodContext} access will work consistently within any method of the advised
	 * object.
	 * <p>
	 * Repository method metadata is also exposed if implementations within the {@link RepositoryFragments repository
	 * composition} implement {@link RepositoryMetadataAccess}.
	 *
	 * @since 3.4
	 * @see RepositoryMethodContext
	 * @see RepositoryMetadataAccess
	 */
	public void setExposeMetadata(boolean exposeMetadata) {
		this.exposeMetadata = exposeMetadata;
	}

	/**
	 * Sets the strategy of how to look up a query to execute finders.
	 *
	 * @param key
	 */
	public void setQueryLookupStrategyKey(@Nullable Key key) {
		this.queryLookupStrategyKey = key;
	}

	/**
	 * Configures a {@link NamedQueries} instance to be handed to the {@link QueryLookupStrategy} for query creation.
	 *
	 * @param namedQueries the namedQueries to set
	 */
	public void setNamedQueries(@Nullable NamedQueries namedQueries) {
		this.namedQueries = namedQueries == null ? PropertiesBasedNamedQueries.EMPTY : namedQueries;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader == null ? ClassUtils.getDefaultClassLoader() : classLoader;
		this.projectionFactory = createProjectionFactory();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		this.projectionFactory = createProjectionFactory();
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {

		if (this.environment == null) {
			this.environment = new StandardEnvironment();
		}

		return this.environment;
	}

	/**
	 * Sets the {@link EvaluationContextProvider} to be used to evaluate SpEL expressions in manually defined queries.
	 *
	 * @param evaluationContextProvider can be {@literal null}, defaults to {@link EvaluationContextProvider#DEFAULT}.
	 */
	public void setEvaluationContextProvider(@Nullable EvaluationContextProvider evaluationContextProvider) {
		this.evaluationContextProvider = evaluationContextProvider == null ? EvaluationContextProvider.DEFAULT
				: evaluationContextProvider;
	}

	/**
	 * Configures the repository base class to use when creating the repository proxy. If not set, the factory will use
	 * the type returned by {@link #getRepositoryBaseClass(RepositoryMetadata)} by default.
	 *
	 * @param repositoryBaseClass the repository base class to back the repository proxy, can be {@literal null}.
	 * @since 1.11
	 */
	public void setRepositoryBaseClass(@Nullable Class<?> repositoryBaseClass) {
		this.repositoryBaseClass = repositoryBaseClass;
	}

	/**
	 * Adds a {@link QueryCreationListener} to the factory to plug in functionality triggered right after creation of
	 * {@link RepositoryQuery} instances.
	 *
	 * @param listener the listener to add.
	 */
	public void addQueryCreationListener(QueryCreationListener<?> listener) {

		Assert.notNull(listener, "Listener must not be null");
		this.queryPostProcessors.add(listener);
	}

	/**
	 * Adds a {@link RepositoryMethodInvocationListener} to the factory to plug in functionality triggered right after
	 * running {@link RepositoryQuery query methods} and {@link Method fragment methods}.
	 *
	 * @param listener the listener to add.
	 * @since 2.4
	 */
	public void addInvocationListener(RepositoryMethodInvocationListener listener) {

		Assert.notNull(listener, "Listener must not be null");
		this.methodInvocationListeners.add(listener);
	}

	/**
	 * Adds {@link RepositoryProxyPostProcessor}s to the factory to allow manipulation of the {@link ProxyFactory} before
	 * the proxy gets created. Note that the {@link QueryExecutorMethodInterceptor} will be added to the proxy
	 * <em>after</em> the {@link RepositoryProxyPostProcessor}s are considered.
	 *
	 * @param processor the post-processor to add.
	 */
	public void addRepositoryProxyPostProcessor(RepositoryProxyPostProcessor processor) {

		Assert.notNull(processor, "RepositoryProxyPostProcessor must not be null");
		this.postProcessors.add(processor);
	}

	/**
	 * Creates {@link RepositoryFragments} based on {@link RepositoryMetadata} to add repository-specific extensions.
	 *
	 * @param metadata the repository metadata to use.
	 * @return fragment composition.
	 */
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
		return RepositoryFragments.empty();
	}

	/**
	 * Returns a repository instance for the given interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @return the implemented repository interface.
	 */
	public <T> T getRepository(Class<T> repositoryInterface) {
		return getRepository(repositoryInterface, RepositoryFragments.empty());
	}

	/**
	 * Returns a repository instance for the given interface backed by a single instance providing implementation logic
	 * for custom logic. For more advanced composition needs use {@link #getRepository(Class, RepositoryFragments)}.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @param customImplementation must not be {@literal null}.
	 * @return the implemented repository interface.
	 */
	public <T> T getRepository(Class<T> repositoryInterface, Object customImplementation) {
		return getRepository(repositoryInterface, RepositoryFragments.just(customImplementation));
	}

	/**
	 * Returns a repository instance for the given interface backed by an instance providing implementation logic for
	 * custom logic.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @param fragments must not be {@literal null}.
	 * @return the implemented repository interface.
	 * @since 2.0
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public <T> T getRepository(Class<T> repositoryInterface, RepositoryFragments fragments) {

		if (logger.isDebugEnabled()) {
			logger.debug(LogMessage.format("Initializing repository instance for %s…", repositoryInterface.getName()));
		}

		Assert.notNull(repositoryInterface, "Repository interface must not be null");
		Assert.notNull(fragments, "RepositoryFragments must not be null");

		ApplicationStartup applicationStartup = getStartup();

		StartupStep repositoryInit = onEvent(applicationStartup, "spring.data.repository.init", repositoryInterface);

		if (repositoryBaseClass != null) {
			repositoryInit.tag("baseClass", repositoryBaseClass.getName());
		}

		StartupStep repositoryMetadataStep = onEvent(applicationStartup, "spring.data.repository.metadata",
				repositoryInterface);
		RepositoryMetadata metadata = getRepositoryMetadata(repositoryInterface);
		repositoryMetadataStep.end();

		StartupStep repositoryCompositionStep = onEvent(applicationStartup, "spring.data.repository.composition",
				repositoryInterface);
		repositoryCompositionStep.tag("fragment.count", String.valueOf(fragments.size()));

		RepositoryStub stub = getRepositoryStub(metadata, fragments);
		RepositoryComposition composition = stub.composition();
		RepositoryInformation information = stub.information();

		repositoryCompositionStep.tag("fragments", () -> {

			StringBuilder fragmentsTag = new StringBuilder();

			for (RepositoryFragment<?> fragment : composition.getFragments()) {

				if (!fragmentsTag.isEmpty()) {
					fragmentsTag.append(";");
				}

				fragmentsTag.append(fragment.getSignatureContributor().getName());
				fragmentsTag.append(fragment.getImplementation().map(it -> ":" + it.getClass().getName()).orElse(""));
			}

			return fragmentsTag.toString();
		});

		repositoryCompositionStep.end();

		StartupStep repositoryTargetStep = onEvent(applicationStartup, "spring.data.repository.target",
				repositoryInterface);
		Object target = getTargetRepository(information);

		repositoryTargetStep.tag("target", target.getClass().getName());
		repositoryTargetStep.end();

		RepositoryComposition compositionToUse = composition.append(RepositoryFragment.implemented(target));
		validate(information, compositionToUse);

		// Create proxy
		StartupStep repositoryProxyStep = onEvent(applicationStartup, "spring.data.repository.proxy", repositoryInterface);
		ProxyFactory result = new ProxyFactory();
		result.setTarget(target);
		result.setInterfaces(repositoryInterface, Repository.class, TransactionalProxy.class);

		if (NullnessMethodInvocationValidator.supports(repositoryInterface)) {
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("Register MethodInvocationValidator for %s…", repositoryInterface.getName()));
			}
			result.addAdvice(new MethodInvocationValidator());
		}

		if (this.exposeMetadata || shouldExposeMetadata(fragments)) {
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("Register ExposeMetadataInterceptor for %s…", repositoryInterface.getName()));
			}
			result.addAdvice(new ExposeMetadataInterceptor(metadata));
			result.addAdvisor(ExposeInvocationInterceptor.ADVISOR);
		}

		if (!postProcessors.isEmpty()) {
			StartupStep repositoryPostprocessorsStep = onEvent(applicationStartup, "spring.data.repository.postprocessors",
					repositoryInterface);
			postProcessors.forEach(processor -> {

				StartupStep singlePostProcessor = onEvent(applicationStartup, "spring.data.repository.postprocessor",
						repositoryInterface);
				singlePostProcessor.tag("type", processor.getClass().getName());
				processor.postProcess(result, information);
				singlePostProcessor.end();
			});
			repositoryPostprocessorsStep.end();
		}

		if (DefaultMethodInvokingMethodInterceptor.hasDefaultMethods(repositoryInterface)) {
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("Register DefaultMethodInvokingMethodInterceptor for %s…",
						repositoryInterface.getName()));
			}
			result.addAdvice(new DefaultMethodInvokingMethodInterceptor());
		}

		Optional<QueryLookupStrategy> queryLookupStrategy = getQueryLookupStrategy(queryLookupStrategyKey,
				getValueExpressionDelegate());
		result.addAdvice(new QueryExecutorMethodInterceptor(information, getProjectionFactory(),
				queryLookupStrategy.orElse(null),
				namedQueries, queryPostProcessors, methodInvocationListeners));

		result.addAdvice(
				new ImplementationMethodExecutionInterceptor(information, compositionToUse, methodInvocationListeners));

		T repository = (T) result.getProxy(classLoader);
		repositoryProxyStep.end();
		repositoryInit.end();

		if (logger.isDebugEnabled()) {
			logger
					.debug(LogMessage.format("Finished creation of repository instance for %s.", repositoryInterface.getName()));
		}

		return repository;
	}

	ValueExpressionDelegate getValueExpressionDelegate() {
		return new ValueExpressionDelegate(
				new QueryMethodValueEvaluationContextAccessor(getEnvironment(), evaluationContextProvider), VALUE_PARSER);
	}

	/**
	 * Returns the {@link ProjectionFactory} to be used with the repository instances created.
	 *
	 * @param classLoader will never be {@literal null}.
	 * @param beanFactory will never be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	protected ProjectionFactory getProjectionFactory(@Nullable ClassLoader classLoader,
			@Nullable BeanFactory beanFactory) {

		SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory(EXPRESSION_PARSER);

		if (classLoader != null) {
			factory.setBeanClassLoader(classLoader);
		}

		if (beanFactory != null) {
			factory.setBeanFactory(beanFactory);
		}

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
		return getRepositoryStub(metadata, fragments).information();
	}

	/**
	 * Returns the cached {@link RepositoryStub} for the given repository and composition. {@link RepositoryMetadata} is a
	 * strong cache key while {@link RepositoryFragments} contributes a light-weight caching component by using only the
	 * fragments hash code. In a typical Spring scenario, that shouldn't impose issues as one repository factory produces
	 * only a single repository instance for one repository interface. Things might be different when using various
	 * fragments for the same repository interface.
	 *
	 * @param metadata
	 * @param fragments
	 * @return
	 */
	private RepositoryStub getRepositoryStub(RepositoryMetadata metadata, RepositoryFragments fragments) {

		RepositoryInformationCacheKey cacheKey = new RepositoryInformationCacheKey(metadata, fragments);

		synchronized (repositoryInformationCache) {

			return repositoryInformationCache.computeIfAbsent(cacheKey, key -> {

				RepositoryComposition composition = RepositoryComposition.fromMetadata(metadata);
				RepositoryFragments repositoryAspects = getRepositoryFragments(metadata);

				composition = composition.append(fragments).append(repositoryAspects);

				Class<?> baseClass = repositoryBaseClass != null ? repositoryBaseClass : getRepositoryBaseClass(metadata);

				return new RepositoryStub(new DefaultRepositoryInformation(metadata, baseClass, composition), composition);
			});
		}
	}

	protected List<QueryMethod> getQueryMethods() {
		return collectingListener.getQueryMethods();
	}

	/**
	 * Returns a {@link ProjectionFactory} instance.
	 *
	 * @return
	 * @since 2.6
	 */
	protected ProjectionFactory getProjectionFactory() {
		return projectionFactory.get();
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 *
	 * @param <T> the entity type
	 * @param <ID> the id type
	 * @param domainClass
	 * @return
	 * @deprecated since 4.0, use {@link #getEntityInformation(RepositoryMetadata)} instead.
	 */
	@Deprecated(since = "4.0")
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		throw new UnsupportedOperationException("getEntityInformation is not implemented");
	}

	/**
	 * Returns the {@link EntityInformation} for the given {@link RepositoryMetadata}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @return the {@link EntityInformation} to be used for {@link RepositoryMetadata}.
	 * @since 4.0
	 */
	public EntityInformation<?, ?> getEntityInformation(RepositoryMetadata metadata) {
		return getEntityInformation(metadata.getDomainType());
	}

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
	 * Returns the {@link QueryLookupStrategy} for the given {@link Key} and {@link ValueExpressionDelegate}.
	 *
	 * @param key can be {@literal null}.
	 * @param valueExpressionDelegate will never be {@literal null}.
	 * @return the {@link QueryLookupStrategy} to use or {@literal null} if no queries should be looked up.
	 * @since 3.4
	 */
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable Key key,
			ValueExpressionDelegate valueExpressionDelegate) {
		return Optional.empty();
	}

	/**
	 * Validates the given repository interface as well as the given custom implementation.
	 *
	 * @param repositoryInformation
	 * @param composition
	 */
	private void validate(RepositoryInformation repositoryInformation, RepositoryComposition composition) {

		RepositoryValidator.validate(composition, getClass(), repositoryInformation);

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
		return instantiateClass(baseClass, constructorArguments);
	}

	/**
	 * Convenience method to instantiate a class using the given {@code constructorArguments} by looking up a matching
	 * constructor.
	 * <p>
	 * Note that this method tries to set the constructor accessible if given a non-accessible (that is, non-public)
	 * constructor, and supports Kotlin classes with optional parameters and default values.
	 *
	 * @param baseClass
	 * @param constructorArguments
	 * @return
	 * @since 2.6
	 */
	@SuppressWarnings("unchecked")
	protected final <R> R instantiateClass(Class<?> baseClass, Object... constructorArguments) {

		Constructor<?> constructor = ReflectionUtils.findConstructor(baseClass, constructorArguments);

		if (constructor == null) {
			throw new IllegalStateException(String.format(
					"No suitable constructor found on %s to match the given arguments: %s. Make sure you implement a constructor taking these",
					baseClass, Arrays.stream(constructorArguments).map(Object::getClass).map(ClassUtils::getQualifiedName)
							.collect(Collectors.joining(", "))));
		}

		return (R) BeanUtils.instantiateClass(constructor, constructorArguments);
	}

	private ApplicationStartup getStartup() {

		try {

			ApplicationStartup applicationStartup = beanFactory != null ? beanFactory.getBean(ApplicationStartup.class)
					: ApplicationStartup.DEFAULT;

			return applicationStartup != null ? applicationStartup : ApplicationStartup.DEFAULT;
		} catch (NoSuchBeanDefinitionException e) {
			return ApplicationStartup.DEFAULT;
		}
	}

	private StartupStep onEvent(ApplicationStartup applicationStartup, String name, Class<?> repositoryInterface) {

		StartupStep step = applicationStartup.start(name);
		return step.tag("repository", repositoryInterface.getName());
	}

	private Lazy<ProjectionFactory> createProjectionFactory() {
		return Lazy.of(() -> getProjectionFactory(this.classLoader, this.beanFactory));
	}

	/**
	 * Checks if at least one {@link RepositoryFragment} indicates need to access to {@link RepositoryMetadata} by being
	 * flagged with {@link RepositoryMetadataAccess}.
	 *
	 * @param fragments
	 * @return {@literal true} if access to metadata is required.
	 */
	private static boolean shouldExposeMetadata(RepositoryFragments fragments) {

		for (RepositoryFragment<?> fragment : fragments) {
			if (fragment.getImplementation().filter(RepositoryMetadataAccess.class::isInstance).isPresent()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method interceptor that calls methods on the {@link RepositoryComposition}.
	 *
	 * @author Mark Paluch
	 */
	static class ImplementationMethodExecutionInterceptor implements MethodInterceptor {

		private final RepositoryComposition composition;
		private final RepositoryInvocationMulticaster invocationMulticaster;

		public ImplementationMethodExecutionInterceptor(RepositoryInformation information,
				RepositoryComposition composition, List<RepositoryMethodInvocationListener> methodInvocationListeners) {
			this.composition = composition;
			this.invocationMulticaster = methodInvocationListeners.isEmpty() ? NoOpRepositoryInvocationMulticaster.INSTANCE
					: new DefaultRepositoryInvocationMulticaster(methodInvocationListeners);
		}

		@Override
		public @Nullable Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();

			try {
				return composition.invoke(invocationMulticaster, method, arguments);
			} catch (Exception ex) {

				if (ex instanceof InvocationTargetException) {
					throw ((InvocationTargetException) ex).getTargetException();
				}

				throw ex;
			}
		}
	}

	/**
	 * Interceptor for repository proxies when the repository needs exposing metadata.
	 */
	private static class ExposeMetadataInterceptor implements MethodInterceptor, Serializable {

		private final RepositoryMetadata repositoryMetadata;

		public ExposeMetadataInterceptor(RepositoryMetadata repositoryMetadata) {
			this.repositoryMetadata = repositoryMetadata;
		}

		@Override
		public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {

			RepositoryMethodContext oldMetadata = null;

			try {

				oldMetadata = RepositoryMethodContextHolder
						.setContext(new DefaultRepositoryMethodContext(repositoryMetadata, invocation.getMethod()));

				return invocation.proceed();

			} finally {
				RepositoryMethodContextHolder.setContext(oldMetadata);
			}
		}
	}

	/**
	 * {@link QueryCreationListener} collecting the {@link QueryMethod}s created for all query methods of the repository
	 * interface.
	 *
	 * @author Oliver Gierke
	 */
	private static class QueryCollectingQueryCreationListener implements QueryCreationListener<RepositoryQuery> {

		/**
		 * All {@link QueryMethod}s.
		 */
		private final List<QueryMethod> queryMethods = new ArrayList<>();

		@Override
		public void onCreation(RepositoryQuery query) {
			this.queryMethods.add(query.getQueryMethod());
		}

		public List<QueryMethod> getQueryMethods() {
			return this.queryMethods;
		}
	}

	/**
	 * Repository stub holding {@link RepositoryInformation} and {@link RepositoryComposition}.
	 *
	 * @param information
	 * @param composition
	 * @author Mark Paluch
	 * @since 3.4.4
	 */
	record RepositoryStub(RepositoryInformation information, RepositoryComposition composition) {

	}

	/**
	 * Simple value object to build up keys to cache {@link RepositoryInformation} instances.
	 *
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	private static final class RepositoryInformationCacheKey {

		private final String repositoryInterfaceName;
		private final long fragmentsHash;

		/**
		 * Creates a new {@link RepositoryInformationCacheKey} for the given {@link RepositoryMetadata} and fragments.
		 *
		 * @param metadata must not be {@literal null}.
		 * @param fragments must not be {@literal null}.
		 */
		public RepositoryInformationCacheKey(RepositoryMetadata metadata, RepositoryFragments fragments) {

			this.repositoryInterfaceName = metadata.getRepositoryInterface().getName();
			this.fragmentsHash = fragments.getFragments().hashCode();
		}

		public String getRepositoryInterfaceName() {
			return this.repositoryInterfaceName;
		}

		public long getFragmentsHash() {
			return this.fragmentsHash;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof RepositoryInformationCacheKey that)) {
				return false;
			}
			if (fragmentsHash != that.fragmentsHash) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(repositoryInterfaceName, that.repositoryInterfaceName);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(repositoryInterfaceName);
			result = 31 * result + Long.hashCode(fragmentsHash);
			return result;
		}

		@Override
		public String toString() {
			return "RepositoryFactorySupport.RepositoryInformationCacheKey(repositoryInterfaceName="
					+ this.getRepositoryInterfaceName() + ", fragmentsHash=" + this.getFragmentsHash() + ")";
		}
	}

	/**
	 * Validator utility to catch common mismatches with a proper error message instead of letting the query mechanism
	 * attempt implementing a query method and fail with a less specific message.
	 */
	static class RepositoryValidator {

		static Map<Class<?>, String> WELL_KNOWN_EXECUTORS = new HashMap<>(4, 1.0f);

		static {

			org.springframework.data.util.ClassUtils.ifPresent("org.springframework.data.querydsl.QuerydslPredicateExecutor",
					RepositoryValidator.class.getClassLoader(), it -> {
						WELL_KNOWN_EXECUTORS.put(it, "Querydsl");
					});

			org.springframework.data.util.ClassUtils.ifPresent(
					"org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor",
					RepositoryValidator.class.getClassLoader(), it -> {
						WELL_KNOWN_EXECUTORS.put(it, "Reactive Querydsl");
					});

			org.springframework.data.util.ClassUtils.ifPresent(
					"org.springframework.data.repository.query.QueryByExampleExecutor",
					RepositoryValidator.class.getClassLoader(), it -> {
						WELL_KNOWN_EXECUTORS.put(it, "Query by Example");
					});

			org.springframework.data.util.ClassUtils.ifPresent(
					"org.springframework.data.repository.query.ReactiveQueryByExampleExecutor",
					RepositoryValidator.class.getClassLoader(), it -> {
						WELL_KNOWN_EXECUTORS.put(it, "Reactive Query by Example");
					});
		}

		/**
		 * Validate the {@link RepositoryComposition} for custom implementations and well-known executors.
		 *
		 * @param composition
		 * @param source
		 * @param repositoryInformation
		 */
		public static void validate(RepositoryComposition composition, Class<?> source,
				RepositoryInformation repositoryInformation) {

			Class<?> repositoryInterface = repositoryInformation.getRepositoryInterface();
			if (repositoryInformation.hasCustomMethod()) {

				if (composition.isEmpty()) {

					throw new IncompleteRepositoryCompositionException(
							String.format("You have custom methods in %s but have not provided a custom implementation",
									org.springframework.util.ClassUtils.getQualifiedName(repositoryInterface)),
							repositoryInterface);
				}

				composition.validateImplementation();
			}

			for (Map.Entry<Class<?>, String> entry : WELL_KNOWN_EXECUTORS.entrySet()) {

				Class<?> executorInterface = entry.getKey();
				if (!executorInterface.isAssignableFrom(repositoryInterface)) {
					continue;
				}

				if (!containsFragmentImplementation(composition, executorInterface)) {
					throw new UnsupportedFragmentException(String.format("Repository %s implements %s but %s does not support %s",
							ClassUtils.getQualifiedName(repositoryInterface), ClassUtils.getQualifiedName(executorInterface),
							ClassUtils.getShortName(source), entry.getValue()), repositoryInterface, executorInterface);
				}
			}
		}

		private static boolean containsFragmentImplementation(RepositoryComposition composition,
				Class<?> executorInterface) {

			for (RepositoryFragment<?> fragment : composition.getFragments()) {

				if (fragment.getImplementation().filter(executorInterface::isInstance).isPresent()) {
					return true;
				}
			}

			return false;
		}
	}
}
