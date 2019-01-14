/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping.callback;

import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * Reactive {@link SimpleEntityCallbacks} implementation.
 *
 * @author Mark Paluch
 */
public class ReactiveEntityCallbacks extends SimpleEntityCallbacks {

	public ReactiveEntityCallbacks() {
		super();
	}

	public ReactiveEntityCallbacks(ApplicationContext beanFactory) {
		super(beanFactory);
	}

	/**
	 * Perform an entity callback.
	 *
	 * @param entity the entity, must not be {@literal null}.
	 * @param callbackType desired callback type.
	 * @param callbackInvoker invocation function for the callback to optionally pass additional parameters.
	 * @return the resulting entity after invoking all callbacks.
	 */
	@SuppressWarnings("unchecked")
	public <T, E extends EntityCallback<T>> Mono<T> callbackLater(T entity, Class<? extends E> callbackType,
			BiFunction<? extends E, T, Publisher<? extends Object>> callbackInvoker) {

		Assert.notNull(entity, "Entity must not be null!");

		ResolvableType resolvedCallbackType = ResolvableType.forClass(callbackType);
		Mono<T> deferredCallbackChain = Mono.just(entity);

		for (EntityCallback<?> callback : getEntityCallbacks(entity, resolvedCallbackType)) {

			deferredCallbackChain = deferredCallbackChain.flatMap(it -> {

				Object o = invokeCallback(callback, it, (BiFunction) callbackInvoker);

				if (o instanceof Publisher) {
					return Mono.from((Publisher<T>) o);
				}

				throw new IllegalStateException("Callback " + callback + " returned a non-Publisher type " + o);

			});
		}

		return deferredCallbackChain;
	}
}
