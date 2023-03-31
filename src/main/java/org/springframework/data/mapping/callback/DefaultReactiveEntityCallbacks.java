/*
 * Copyright 2019-2023 the original author or authors.
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

import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ReactiveEntityCallbacks} implementation.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael J. Simons
 */
class DefaultReactiveEntityCallbacks implements ReactiveEntityCallbacks {

	private final Map<Class<?>, Method> callbackMethodCache = new ConcurrentReferenceHashMap<>(64);
	private final ReactiveEntityCallbackInvoker callbackInvoker = new DefaultReactiveEntityCallbackInvoker();
	private final EntityCallbackDiscoverer callbackDiscoverer;

	/**
	 * Create new instance of {@link DefaultReactiveEntityCallbacks}.
	 */
	DefaultReactiveEntityCallbacks() {
		this.callbackDiscoverer = new EntityCallbackDiscoverer();
	}

	/**
	 * Create new instance of {@link DefaultReactiveEntityCallbacks} discovering {@link EntityCallback entity callbacks}
	 * within the given {@link BeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 */
	DefaultReactiveEntityCallbacks(BeanFactory beanFactory) {
		this.callbackDiscoverer = new EntityCallbackDiscoverer(beanFactory);
	}

	@Override
	public <T> Mono<T> callback(Class<? extends EntityCallback> callbackType, T entity, Object... args) {

		Assert.notNull(entity, "Entity must not be null");

		Class<T> entityType = (Class<T>) (entity != null ? ClassUtils.getUserClass(entity.getClass())
				: callbackDiscoverer.resolveDeclaredEntityType(callbackType).getRawClass());

		Method callbackMethod = callbackMethodCache.computeIfAbsent(callbackType, it -> {

			Method method = EntityCallbackDiscoverer.lookupCallbackMethod(it, entityType, args);
			ReflectionUtils.makeAccessible(method);
			return method;
		});

		Mono<T> deferredCallbackChain = Mono.just(entity);

		for (EntityCallback<T> callback : callbackDiscoverer.getEntityCallbacks(entityType,
				ResolvableType.forClass(callbackType))) {

			BiFunction<EntityCallback<T>, T, Object> callbackFunction = EntityCallbackDiscoverer
					.computeCallbackInvokerFunction(callback, callbackMethod, args);

			deferredCallbackChain = deferredCallbackChain
					.flatMap(it -> callbackInvoker.invokeCallback(callback, it, callbackFunction));
		}

		return deferredCallbackChain;
	}

	@Override
	public void addEntityCallback(EntityCallback<?> callback) {
		this.callbackDiscoverer.addEntityCallback(callback);
	}

	static class DefaultReactiveEntityCallbackInvoker implements ReactiveEntityCallbackInvoker {

		@Override
		public <T> Mono<T> invokeCallback(EntityCallback<T> callback, T entity,
				BiFunction<EntityCallback<T>, T, Object> callbackInvokerFunction) {

			try {

				Object value = callbackInvokerFunction.apply(callback, entity);

				if (value != null) {
					return value instanceof Publisher ? Mono.from((Publisher<T>) value) : Mono.just((T) value);
				}

				throw new IllegalArgumentException(
						String.format("Callback invocation on %s returned null value for %s", callback.getClass(), entity));
			} catch (IllegalArgumentException | ClassCastException ex) {

				String msg = ex.getMessage();
				if (msg == null || EntityCallbackInvoker.matchesClassCastMessage(msg, entity.getClass())) {

					// Possibly a lambda-defined listener which we could not resolve the generic event type for
					// -> let's suppress the exception and just log a debug message.
					Log logger = LogFactory.getLog(getClass());
					if (logger.isDebugEnabled()) {
						logger.debug("Non-matching callback type for entity callback: " + callback, ex);
					}

					return Mono.just(entity);
				} else {
					return Mono.error(ex);
				}
			}
		}
	}
}
