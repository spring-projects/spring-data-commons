/*
 * Copyright 2019-present the original author or authors.
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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.comparator.Comparators;

/**
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Myeonghyeon Lee
 * @author Johannes Englmeier
 * @author Oliver Drotbohm
 * @since 2.2
 */
class EntityCallbackDiscoverer {

	private final CallbackRetriever defaultRetriever = new CallbackRetriever();
	private final Map<CallbackCacheKey, CallbackRetriever> retrieverCache = new ConcurrentHashMap<>(64);

	private @Nullable ClassLoader beanClassLoader;

	/**
	 * Create a new {@link EntityCallback} instance.
	 */
	EntityCallbackDiscoverer() {}

	/**
	 * Create a new {@link EntityCallback} instance. <p Pre-loads {@link EntityCallback} beans by scanning the
	 * {@link BeanFactory}.
	 */
	EntityCallbackDiscoverer(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}

	void addEntityCallback(EntityCallback<?> callback) {

		Assert.notNull(callback, "Callback must not be null");

		synchronized (this.defaultRetriever) {

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	<T extends S, S> Collection<EntityCallback<S>> getEntityCallbacks(Class<T> entity, ResolvableType callbackType) {

		CallbackCacheKey cacheKey = new CallbackCacheKey(callbackType, entity);

		// Quick check for existing entry on ConcurrentHashMap...
		CallbackRetriever retriever = this.retrieverCache.get(cacheKey);
		if (retriever != null) {
			return (Collection) retriever.getEntityCallbacks();
		}

		if (this.beanClassLoader == null || ClassUtils.isCacheSafe(entity, this.beanClassLoader)
				&& ClassUtils.isCacheSafe(entity, this.beanClassLoader)) {

			// Fully synchronized building and caching of a CallbackRetriever
			synchronized (this.defaultRetriever) {
				retriever = this.retrieverCache.get(cacheKey);
				if (retriever != null) {
					return (Collection) retriever.getEntityCallbacks();
				}
				retriever = new CallbackRetriever();
				Collection<EntityCallback<?>> callbacks = retrieveEntityCallbacks(ResolvableType.forClass(entity),
						callbackType, retriever);
				this.retrieverCache.put(cacheKey, retriever);
				return (Collection) callbacks;
			}
		} else {
			// No CallbackRetriever caching -> no synchronization necessary
			return (Collection) retrieveEntityCallbacks(callbackType, callbackType, null);
		}
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

		synchronized (this.defaultRetriever) {
			callbacks = new LinkedHashSet<>(this.defaultRetriever.entityCallbacks);
		}

		for (EntityCallback<?> callback : callbacks) {

			if (supportsEvent(callback, entityType, callbackType)) {

				callback = callback instanceof EntityCallbackAdapter<?> adapter ? adapter.delegate() : callback;

				if (retriever != null) {
					retriever.getEntityCallbacks().add(callback);
				}

				allCallbacks.add(callback);
			}
		}

		AnnotationAwareOrderComparator.sort(allCallbacks);

		if (retriever != null) {
			retriever.entityCallbacks.clear();
			retriever.entityCallbacks.addAll(allCallbacks);
		}

		return allCallbacks;
	}

	/**
	 * Set the {@link BeanFactory} and optionally class loader if not set. Pre-loads {@link EntityCallback} beans by
	 * scanning the {@link BeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {

		if (beanFactory instanceof ConfigurableBeanFactory cbf) {

			if (this.beanClassLoader == null) {
				this.beanClassLoader = cbf.getBeanClassLoader();
			}
		}

		defaultRetriever.discoverEntityCallbacks(beanFactory);
		this.retrieverCache.clear();
	}

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

		throw new IllegalStateException("%s does not define a callback method accepting %s and %s additional arguments"
				.formatted(ClassUtils.getShortName(callbackType), ClassUtils.getShortName(entityType), args.length));
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

	/**
	 * Filter a callback early through checking its generically declared entity type before trying to instantiate it.
	 * <p>
	 * If this method returns {@literal true} for a given callback as a first pass, the callback instance will get
	 * retrieved and fully evaluated through a {@link #supportsEvent(EntityCallback, ResolvableType, ResolvableType)} call
	 * afterwards.
	 *
	 * @param callbackType the callback's type as determined by the BeanFactory.
	 * @param entityType the entity type to check.
	 * @return whether the given callback should be included in the candidates for the given callback type.
	 */
	static boolean supportsEvent(ResolvableType callbackType, ResolvableType entityType) {
		return callbackType.as(EntityCallback.class).getGeneric(0).isAssignableFrom(entityType);
	}

	/**
	 * Determine whether the given callback supports the given entity type and callback type.
	 *
	 * @param callback the target callback to check.
	 * @param entityType the entity type to check.
	 * @param callbackType the source type to check against.
	 * @return whether the given callback should be included in the candidates for the given callback type.
	 */
	static boolean supportsEvent(EntityCallback<?> callback, ResolvableType entityType, ResolvableType callbackType) {

		return callback instanceof EntityCallbackAdapter<?> provider ? provider.supports(callbackType, entityType)
				: callbackType.isInstance(callback) && supportsEvent(ResolvableType.forInstance(callback), entityType);
	}

	/**
	 * Helper class that encapsulates a specific set of target {@link EntityCallback callbacks}, allowing for efficient
	 * retrieval of pre-filtered callbacks.
	 */
	private static class CallbackRetriever {

		private final Set<EntityCallback<?>> entityCallbacks = new LinkedHashSet<>();

		Collection<EntityCallback<?>> getEntityCallbacks() {
			return this.entityCallbacks;
		}

		@SuppressWarnings("rawtypes")
		void discoverEntityCallbacks(BeanFactory beanFactory) {

			// We need both a ListableBeanFactory and BeanDefinitionRegistry here for advanced inspection.
			// If we don't get that, use simple inspection.
			if (!(beanFactory instanceof ConfigurableListableBeanFactory bf)) {
				beanFactory.getBeanProvider(EntityCallback.class).stream().forEach(entityCallbacks::add);
				return;
			}

			for (var beanName : bf.getBeanNamesForType(EntityCallback.class)) {

				EntityCallback<?> bean = (EntityCallback) bf.getBean(beanName);

				ResolvableType type = ResolvableType.forClass(EntityCallback.class, bean.getClass());
				ResolvableType entityType = type.getGeneric(0);

				if (entityType.resolve() != null) {
					entityCallbacks.add(bean);
				} else {

					BeanDefinition definition = bf.getMergedBeanDefinition(beanName);
					entityCallbacks.add(new EntityCallbackAdapter<>(bean, definition.getResolvableType()));
				}
			}
		}
	}

	/**
	 * A combination of an {@link EntityCallback}
	 *
	 * @author Oliver Drotbohm
	 */
	private record EntityCallbackAdapter<T>(EntityCallback<T> delegate,
			ResolvableType type) implements EntityCallback<T> {

		boolean supports(ResolvableType callbackType, ResolvableType entityType) {
			return callbackType.isInstance(delegate) && supportsEvent(type, entityType);
		}
	}

	/**
	 * Cache key for {@link EntityCallback}, based on event type and source type.
	 */
	private static final class CallbackCacheKey implements Comparable<CallbackCacheKey> {

		private static final Comparator<CallbackCacheKey> COMPARATOR = Comparators.<CallbackCacheKey> nullsHigh() //
				.thenComparing(it -> it.callbackType.toString()) //
				.thenComparing(it -> it.entityType.getName());

		private final ResolvableType callbackType;
		private final Class<?> entityType;

		private CallbackCacheKey(ResolvableType callbackType, Class<?> entityType) {

			this.callbackType = callbackType;
			this.entityType = entityType;
		}

		@Override
		public int compareTo(@Nullable CallbackCacheKey other) {
			return COMPARATOR.compare(this, other);
		}

		@Override
		public boolean equals(@Nullable Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof CallbackCacheKey that)) {
				return false;
			}

			return Objects.equals(this.callbackType, that.callbackType)
					&& Objects.equals(this.entityType, that.entityType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(callbackType, entityType);
		}

		@Override
		public String toString() {

			return "CallbackCacheKey[" +
					"callbackType=" + callbackType + ", " +
					"entityType=" + entityType + ']';
		}
	}
}
