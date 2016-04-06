/*
 * Copyright 2008-2016 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
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
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Factory bean to create instances of a given repository interface. Creates a proxy implementing the configured
 * repository interface and apply an advice handing the control to the {@code QueryExecuterMethodInterceptor}. Query
 * detection strategy can be configured by setting {@link QueryLookupStrategy.Key}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public abstract class RepositoryFactorySupport implements BeanClassLoaderAware, BeanFactoryAware {

	private static final boolean IS_JAVA_8 = org.springframework.util.ClassUtils.isPresent("java.util.Optional",
			RepositoryFactorySupport.class.getClassLoader());
	private static final Class<?> TRANSACTION_PROXY_TYPE = getTransactionProxyType();

	private final Map<RepositoryInformationCacheKey, RepositoryInformation> repositoryInformationCache = new HashMap<RepositoryInformationCacheKey, RepositoryInformation>();
	private final List<RepositoryProxyPostProcessor> postProcessors = new ArrayList<RepositoryProxyPostProcessor>();

	private Class<?> repositoryBaseClass;
	private QueryLookupStrategy.Key queryLookupStrategyKey;
	private List<QueryCreationListener<?>> queryPostProcessors = new ArrayList<QueryCreationListener<?>>();
	private NamedQueries namedQueries = PropertiesBasedNamedQueries.EMPTY;
	private ClassLoader classLoader = org.springframework.util.ClassUtils.getDefaultClassLoader();
	private EvaluationContextProvider evaluationContextProvider = DefaultEvaluationContextProvider.INSTANCE;
	private BeanFactory beanFactory;
	private ConversionService conversionService;

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

	/**
	 * Configures a {@link ConversionService} instance to convert method parameters when calling implementation methods on
	 * the base class or a custom implementation.
	 *
	 * @param conversionService the conversionService to set.
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
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
		this.repositoryBaseClass = repositoryBaseClass;
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

		result.addAdvice(ExposeInvocationInterceptor.INSTANCE);

		if (TRANSACTION_PROXY_TYPE != null) {
			result.addInterface(TRANSACTION_PROXY_TYPE);
		}

		for (RepositoryProxyPostProcessor processor : postProcessors) {
			processor.postProcess(result, information);
		}

		if (IS_JAVA_8) {
			result.addAdvice(new DefaultMethodInvokingMethodInterceptor());
		}

		result.addAdvice(new QueryExecutorMethodInterceptor(information));

		if (conversionService == null) {
			result.addAdvice(new ImplementationMethodExecutionInterceptor(information, customImplementation, target));
		} else {
			result.addAdvice(new ConvertingImplementationMethodExecutionInterceptor(information, customImplementation, target,
					conversionService));
		}

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
			Class<?> customImplementationClass) {

		RepositoryInformationCacheKey cacheKey = new RepositoryInformationCacheKey(metadata, customImplementationClass);
		RepositoryInformation repositoryInformation = repositoryInformationCache.get(cacheKey);

		if (repositoryInformation != null) {
			return repositoryInformation;
		}

		Class<?> repositoryBaseClass = this.repositoryBaseClass == null ? getRepositoryBaseClass(metadata)
				: this.repositoryBaseClass;

		if (conversionService == null) {
			repositoryInformation = new DefaultRepositoryInformation(metadata, repositoryBaseClass,
					customImplementationClass);
		} else {
			// TODO: Not sure this is the best idea but at some point we need to distinguish between
			// methods that want to get unwrapped data from wrapped parameters and those which are
			// just fine with wrappers because at some point a converting interceptor kicks in
			repositoryInformation = new ReactiveRepositoryInformation(metadata, repositoryBaseClass,
					customImplementationClass, conversionService);
		}

		repositoryInformationCache.put(cacheKey, repositoryInformation);
		return repositoryInformation;
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
	 * Returns the {@link QueryLookupStrategy} for the given {@link Key}.
	 * 
	 * @deprecated favor {@link #getQueryLookupStrategy(Key, EvaluationContextProvider)}
	 * @param key can be {@literal null}
	 * @return the {@link QueryLookupStrategy} to use or {@literal null} if no queries should be looked up.
	 */
	protected QueryLookupStrategy getQueryLookupStrategy(Key key) {
		return null;
	}

	/**
	 * Returns the {@link QueryLookupStrategy} for the given {@link Key} and {@link EvaluationContextProvider}.
	 * 
	 * @param key can be {@literal null}.
	 * @param evaluationContextProvider will never be {@literal null}.
	 * @return the {@link QueryLookupStrategy} to use or {@literal null} if no queries should be looked up.
	 * @since 1.9
	 */
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
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

			throw new IllegalArgumentException(
					String.format("You have custom methods in %s but not provided a custom implementation!",
							repositoryInformation.getRepositoryInterface()));
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
	@SuppressWarnings("unchecked")
	protected final <R> R getTargetRepositoryViaReflection(RepositoryInformation information,
			Object... constructorArguments) {

		Class<?> baseClass = information.getRepositoryBaseClass();
		Constructor<?> constructor = ReflectionUtils.findConstructor(baseClass, constructorArguments);

		if (constructor == null) {

			List<Class<?>> argumentTypes = new ArrayList<Class<?>>(constructorArguments.length);

			for (Object argument : constructorArguments) {
				argumentTypes.add(argument.getClass());
			}

			throw new IllegalStateException(String.format(
					"No suitable constructor found on %s to match the given arguments: %s. Make sure you implement a constructor taking these",
					baseClass, argumentTypes));
		}

		return (R) BeanUtils.instantiateClass(constructor, constructorArguments);
	}

	/**
	 * Returns the TransactionProxy type or {@literal null} if not on the classpath.
	 * 
	 * @return
	 */
	private static Class<?> getTransactionProxyType() {

		try {
			return org.springframework.util.ClassUtils
					.forName("org.springframework.transaction.interceptor.TransactionalProxy", null);
		} catch (ClassNotFoundException o_O) {
			return null;
		}
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

		private final QueryExecutionResultHandler resultHandler;

		/**
		 * Creates a new {@link QueryExecutorMethodInterceptor}. Builds a model of {@link QueryMethod}s to be invoked on
		 * execution of repository interface methods.
		 */
		public QueryExecutorMethodInterceptor(RepositoryInformation repositoryInformation) {

			this.resultHandler = new QueryExecutionResultHandler();

			QueryLookupStrategy lookupStrategy = getQueryLookupStrategy(queryLookupStrategyKey,
					RepositoryFactorySupport.this.evaluationContextProvider);
			lookupStrategy = lookupStrategy == null ? getQueryLookupStrategy(queryLookupStrategyKey) : lookupStrategy;
			Iterable<Method> queryMethods = repositoryInformation.getQueryMethods();

			if (lookupStrategy == null) {

				if (queryMethods.iterator().hasNext()) {
					throw new IllegalStateException("You have defined query method in the repository but "
							+ "you don't have any query lookup strategy defined. The "
							+ "infrastructure apparently does not support query methods!");
				}

				return;
			}

			SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory();
			factory.setBeanClassLoader(classLoader);
			factory.setBeanFactory(beanFactory);

			for (Method method : queryMethods) {

				RepositoryQuery query = lookupStrategy.resolveQuery(method, repositoryInformation, factory, namedQueries);

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
	public class ImplementationMethodExecutionInterceptor implements MethodInterceptor {

		private final Object customImplementation;
		private final RepositoryInformation repositoryInformation;
		private final Object target;

		/**
		 * Creates a new {@link QueryExecutorMethodInterceptor}. Builds a model of {@link QueryMethod}s to be invoked on
		 * execution of repository interface methods.
		 */
		public ImplementationMethodExecutionInterceptor(RepositoryInformation repositoryInformation,
				Object customImplementation, Object target) {

			Assert.notNull(repositoryInformation, "RepositoryInformation must not be null!");
			Assert.notNull(target, "Target must not be null!");

			this.repositoryInformation = repositoryInformation;
			this.customImplementation = customImplementation;
			this.target = target;

		}

		/* (non-Javadoc)
		 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
		 */
		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {

			Method method = invocation.getMethod();
			Object[] arguments = invocation.getArguments();

			if (isCustomMethodInvocation(invocation)) {

				Method actualMethod = repositoryInformation.getTargetClassMethod(method);
				return executeMethodOn(customImplementation, actualMethod, arguments);
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

			if (null == customImplementation) {
				return false;
			}

			return repositoryInformation.isCustomMethod(invocation.getMethod());
		}
	}

	/**
	 * Method interceptor that converts parameters before invoking a method.
	 *
	 * @author Mark Paluch
	 */
	public class ConvertingImplementationMethodExecutionInterceptor extends ImplementationMethodExecutionInterceptor {

		private final ConversionService conversionService;

		/**
		 * @param repositoryInformation
		 * @param customImplementation
		 * @param target
		 * @param conversionService
		 */
		public ConvertingImplementationMethodExecutionInterceptor(RepositoryInformation repositoryInformation,
				Object customImplementation, Object target, ConversionService conversionService) {
			super(repositoryInformation, customImplementation, target);
			this.conversionService = conversionService;
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

				if (parameterTypes[i].isAssignableFrom(parameters[i].getClass())
						|| !conversionService.canConvert(parameters[i].getClass(), parameterTypes[i])) {

					result[i] = parameters[i];

				} else {
					result[i] = conversionService.convert(parameters[i], parameterTypes[i]);
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

	/**
	 * Simple value object to build up keys to cache {@link RepositoryInformation} instances.
	 * 
	 * @author Oliver Gierke
	 */
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
		public RepositoryInformationCacheKey(RepositoryMetadata metadata, Class<?> customImplementationType) {
			this.repositoryInterfaceName = metadata.getRepositoryInterface().getName();
			this.customImplementationClassName = customImplementationType == null ? null : customImplementationType.getName();
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof RepositoryInformationCacheKey)) {
				return false;
			}

			RepositoryInformationCacheKey that = (RepositoryInformationCacheKey) obj;
			return this.repositoryInterfaceName.equals(that.repositoryInterfaceName)
					&& ObjectUtils.nullSafeEquals(this.customImplementationClassName, that.customImplementationClassName);
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = 31;

			result += 17 * repositoryInterfaceName.hashCode();
			result += 17 * ObjectUtils.nullSafeHashCode(customImplementationClassName);

			return result;
		}
	}
}
