/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.mapping.callback;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.comparator.Comparators;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Myeonghyeon Lee
 * @since 2.2
 */
class EntityCallbackDiscoverer {

	private final CallbackRetriever defaultRetriever = new CallbackRetriever(false);
	private final Map<CallbackCacheKey, CallbackRetriever> retrieverCache = new ConcurrentHashMap<>(64);
	private final Map<Class<?>, ResolvableType> entityTypeCache = new ConcurrentReferenceHashMap<>(64);

	@Nullable private ClassLoader beanClassLoader;
	@Nullable private BeanFactory beanFactory;

	private Object retrievalMutex = this.defaultRetriever;

	/**
	 * Create a new {@link EntityCallback} instance.
	 */
	EntityCallbackDiscoverer() {}

	/**
	 * Create a new {@link EntityCallback} instance.
	 * <p />
	 * Pre loads {@link EntityCallback} beans by scanning the {@link BeanFactory}.
	 */
	EntityCallbackDiscoverer(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}

	void addEntityCallback(EntityCallback<?> callback) {

		Assert.notNull(callback, "Callback must not be null!");

		synchronized (this.retrievalMutex) {

			// Explicitly remove target for a proxy, if registered already,
			// in order to avoid double invocations of the same callback.
			Object singletonTarget = AopProxyUtils.getSingletonTarget(callback);
			if (singletonTarget instanceof EntityCallback) {
				this.defaultRetriever.entityCallbacks.remove(singletonTarget);
			}
			this.defaultRetriever.entityCallbacks.add(callback);
			this.retrieverCache.clear();
		}
	}

	void addEntityCallbackBean(String callbackBeanName) {

		synchronized (this.retrievalMutex) {
			this.defaultRetriever.entityCallbackBeans.add(callbackBeanName);
			this.retrieverCache.clear();
		}
	}

	void removeEntityCallback(EntityCallback<?> callback) {

		synchronized (this.retrievalMutex) {
			this.defaultRetriever.entityCallbacks.remove(callback);
			this.retrieverCache.clear();
		}
	}

	void removeEntityCallbackBean(String callbackBeanName) {

		synchronized (this.retrievalMutex) {
			this.defaultRetriever.entityCallbackBeans.remove(callbackBeanName);
			this.retrieverCache.clear();
		}
	}

	void clear() {

		synchronized (this.retrievalMutex) {
			this.defaultRetriever.entityCallbacks.clear();
			this.defaultRetriever.entityCallbackBeans.clear();
			this.retrieverCache.clear();
		}
	}

	/**
	 * Return a {@link Collection} of all {@link EntityCallback}s matching the given entity type. Non-matching callbacks
	 * get excluded early.
	 *
	 * @param entity the entity to be called back for. Allows for excluding non-matching callbacks early, based on cached
	 *          matching information.
	 * @param callbackType the source callback type.
	 * @return a {@link Collection} of {@link EntityCallback}s.
	 * @see EntityCallback
	 */
	<T extends S, S> Collection<EntityCallback<S>> getEntityCallbacks(Class<T> entity, ResolvableType callbackType) {

		Class<?> sourceType = entity;
		CallbackCacheKey cacheKey = new CallbackCacheKey(callbackType, sourceType);

		// Quick check for existing entry on ConcurrentHashMap...
		CallbackRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return (Collection<EntityCallback<S>>) (Collection) retriever.getEntityCallbacks();
		}

		if (this.beanClassLoader == null || (ClassUtils.isCacheSafe(entity.getClass(), this.beanClassLoader)
				&& (sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {

			// Fully synchronized building and caching of a CallbackRetriever
			synchronized (this.retrievalMutex) {
				retriever = this.retrieverCache.get(cacheKey);
				if (retriever != null) {
					return (Collection<EntityCallback<S>>) (Collection) retriever.getEntityCallbacks();
				}
				retriever = new CallbackRetriever(true);
				Collection<EntityCallback<?>> callbacks = retrieveEntityCallbacks(ResolvableType.forClass(sourceType),
						callbackType, retriever);
				this.retrieverCache.put(cacheKey, retriever);
				return (Collection<EntityCallback<S>>) (Collection) callbacks;
			}
		} else {
			// No CallbackRetriever caching -> no synchronization necessary
			return (Collection<EntityCallback<S>>) (Collection) retrieveEntityCallbacks(callbackType, callbackType, null);
		}
	}

	@Nullable
	ResolvableType resolveDeclaredEntityType(Class<?> callbackType) {

		ResolvableType eventType = entityTypeCache.get(callbackType);

		if (eventType == null) {
			eventType = ResolvableType.forClass(callbackType).as(EntityCallback.class).getGeneric();
			entityTypeCache.put(callbackType, eventType);
		}

		return (eventType != ResolvableType.NONE ? eventType : null);
	}

	/**
	 * Actually retrieve the callbacks for the given entity and callback type.
	 *
	 * @param entityType the entity type.
	 * @param callbackType the source callback type.
	 * @param retriever the {@link CallbackRetriever}, if supposed to populate one (for caching purposes)
	 * @return the pre-filtered list of entity callbacks for the given entity and callback type.
	 */
	private Collection<EntityCallback<?>> retrieveEntityCallbacks(ResolvableType entityType, ResolvableType callbackType,
			@Nullable CallbackRetriever retriever) {

		List<EntityCallback<?>> allCallbacks = new ArrayList<>();
		Set<EntityCallback<?>> callbacks;
		Set<String> callbackBeans;

		synchronized (this.retrievalMutex) {
			callbacks = new LinkedHashSet<>(this.defaultRetriever.entityCallbacks);
			callbackBeans = new LinkedHashSet<>(this.defaultRetriever.entityCallbackBeans);
		}

		for (EntityCallback<?> callback : callbacks) {
			if (supportsEvent(callback, entityType, callbackType)) {
				if (retriever != null) {
					retriever.getEntityCallbacks().add(callback);
				}
				allCallbacks.add(callback);
			}
		}

		if (!callbackBeans.isEmpty()) {
			BeanFactory beanFactory = getRequiredBeanFactory();
			for (String callbackBeanName : callbackBeans) {
				try {
					Class<?> callbackImplType = beanFactory.getType(callbackBeanName);
					if (callbackImplType == null || supportsEvent(callbackImplType, entityType)) {
						EntityCallback<?> callback = beanFactory.getBean(callbackBeanName, EntityCallback.class);
						if (!allCallbacks.contains(callback) && supportsEvent(callback, entityType, callbackType)) {
							if (retriever != null) {
								if (beanFactory.isSingleton(callbackBeanName)) {
									retriever.entityCallbacks.add(callback);
								} else {
									retriever.entityCallbackBeans.add(callbackBeanName);
								}
							}
							allCallbacks.add(callback);
						}
					}
				} catch (NoSuchBeanDefinitionException ex) {
					// Singleton callback instance (without backing bean definition) disappeared -
					// probably in the middle of the destruction phase
				}
			}
		}

		AnnotationAwareOrderComparator.sort(allCallbacks);

		if (retriever != null && retriever.entityCallbackBeans.isEmpty()) {
			retriever.entityCallbacks.clear();
			retriever.entityCallbacks.addAll(allCallbacks);
		}

		return allCallbacks;
	}

	/**
	 * Filter a callback early through checking its generically declared entity type before trying to instantiate it.
	 * <p>
	 * If this method returns {@literal true} for a given callback as a first pass, the callback instance will get
	 * retrieved and fully evaluated through a {@link #supportsEvent(EntityCallback, ResolvableType, ResolvableType)} call
	 * afterwards.
	 *
	 * @param callback the callback's type as determined by the BeanFactory.
	 * @param entityType the entity type to check.
	 * @return whether the given callback should be included in the candidates for the given callback type.
	 */
	protected boolean supportsEvent(Class<?> callback, ResolvableType entityType) {

		ResolvableType declaredEventType = resolveDeclaredEntityType(callback);
		return (declaredEventType == null || declaredEventType.isAssignableFrom(entityType));
	}

	/**
	 * Determine whether the given callback supports the given entity type and callback type.
	 *
	 * @param callback the target callback to check.
	 * @param entityType the entity type to check.
	 * @param callbackType the source type to check against.
	 * @return whether the given callback should be included in the candidates for the given callback type.
	 */
	protected boolean supportsEvent(EntityCallback<?> callback, ResolvableType entityType, ResolvableType callbackType) {

		return supportsEvent(callback.getClass(), entityType)
				&& callbackType.isAssignableFrom(ResolvableType.forInstance(callback));
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see org.springframework.beans.factory.BeanClassLoaderAware
	 */
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * Set the {@link BeanFactory} and optionally {@link #setBeanClassLoader(ClassLoader) class loader} if not set. Pre
	 * loads {@link EntityCallback} beans by scanning the {@link BeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {

		this.beanFactory = beanFactory;

		if (beanFactory instanceof ConfigurableBeanFactory) {
			ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) beanFactory;
			if (this.beanClassLoader == null) {
				this.beanClassLoader = cbf.getBeanClassLoader();
			}
			this.retrievalMutex = cbf.getSingletonMutex();
		}

		defaultRetriever.discoverEntityCallbacks(this.beanFactory);
		this.retrieverCache.clear();
	}

	@Nullable
	static Method lookupCallbackMethod(Class<?> callbackType, Class<?> entityType, Object[] args) {

		Collection<Method> methods = new ArrayList<>(1);

		ReflectionUtils.doWithMethods(callbackType, methods::add, method -> {

			if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != args.length + 1
					|| method.isBridge() || ReflectionUtils.isObjectMethod(method)) {
				return false;
			}

			return ClassUtils.isAssignable(method.getParameterTypes()[0], entityType);
		});

		if (methods.size() == 1) {
			return methods.iterator().next();
		}

		throw new IllegalStateException(
				String.format("%s does not define a callback method accepting %s and %s additional arguments.",
						ClassUtils.getShortName(callbackType), ClassUtils.getShortName(entityType), args.length));
	}

	static <T> BiFunction<EntityCallback<T>, T, Object> computeCallbackInvokerFunction(EntityCallback<T> callback,
			Method callbackMethod, Object[] args) {

		return (entityCallback, entity) -> {

			Object[] invocationArgs = new Object[args.length + 1];
			invocationArgs[0] = entity;
			if (args.length > 0) {
				System.arraycopy(args, 0, invocationArgs, 1, args.length);
			}

			return ReflectionUtils.invokeMethod(callbackMethod, callback, invocationArgs);
		};
	}

	private BeanFactory getRequiredBeanFactory() {

		Assert.state(beanFactory != null,
				"EntityCallbacks cannot retrieve callback beans because it is not associated with a BeanFactory");
		return beanFactory;
	}

	/**
	 * Helper class that encapsulates a specific set of target {@link EntityCallback callbacks}, allowing for efficient
	 * retrieval of pre-filtered callbacks.
	 */
	class CallbackRetriever {

		private final Set<EntityCallback<?>> entityCallbacks = new LinkedHashSet<>();

		private final Set<String> entityCallbackBeans = new LinkedHashSet<>();

		private final boolean preFiltered;

		CallbackRetriever(boolean preFiltered) {
			this.preFiltered = preFiltered;
		}

		Collection<EntityCallback<?>> getEntityCallbacks() {

			List<EntityCallback<?>> allCallbacks = new ArrayList<>(
					this.entityCallbacks.size() + this.entityCallbackBeans.size());
			allCallbacks.addAll(this.entityCallbacks);

			if (!this.entityCallbackBeans.isEmpty()) {
				BeanFactory beanFactory = getRequiredBeanFactory();
				for (String callbackBeanName : this.entityCallbackBeans) {
					try {
						EntityCallback<?> callback = beanFactory.getBean(callbackBeanName, EntityCallback.class);
						if (this.preFiltered || !allCallbacks.contains(callback)) {
							allCallbacks.add(callback);
						}
					} catch (NoSuchBeanDefinitionException ex) {
						// Singleton callback instance (without backing bean definition) disappeared -
						// probably in the middle of the destruction phase
					}
				}
			}

			if (!this.preFiltered || !this.entityCallbackBeans.isEmpty()) {
				AnnotationAwareOrderComparator.sort(allCallbacks);
			}

			return allCallbacks;
		}

		void discoverEntityCallbacks(BeanFactory beanFactory) {
			beanFactory.getBeanProvider(EntityCallback.class).stream().forEach(entityCallbacks::add);
		}
	}

	/**
	 * Cache key for {@link EntityCallback}, based on event type and source type.
	 */
	static final class CallbackCacheKey implements Comparable<CallbackCacheKey> {

		private final ResolvableType callbackType;

		private final Class<?> entityType;

		public CallbackCacheKey(ResolvableType callbackType, @Nullable Class<?> entityType) {

			Assert.notNull(callbackType, "Callback type must not be null");
			Assert.notNull(entityType, "Entity type must not be null");

			this.callbackType = callbackType;
			this.entityType = entityType;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object other) {

			if (this == other) {
				return true;
			}

			CallbackCacheKey otherKey = (CallbackCacheKey) other;

			return (this.callbackType.equals(otherKey.callbackType)
					&& ObjectUtils.nullSafeEquals(this.entityType, otherKey.entityType));
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return this.callbackType.hashCode() * 17 + ObjectUtils.nullSafeHashCode(this.entityType);
		}

		@Override
		public int compareTo(CallbackCacheKey other) {

			return Comparators.<CallbackCacheKey> nullsHigh().thenComparing(it -> callbackType.toString())
					.thenComparing(it -> entityType.getName()).compare(this, other);

		}
	}

}
