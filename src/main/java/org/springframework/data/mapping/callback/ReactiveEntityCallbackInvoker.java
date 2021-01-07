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

import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2.2
 */
interface ReactiveEntityCallbackInvoker extends EntityCallbackInvoker {

	/**
	 * @param callback must not be {@literal null}.
	 * @param entity can be {@literal null}
	 * @param callbackInvokerFunction must not be {@literal null}.
	 * @param <T>
	 * @return a {@link Mono} emitting the result of the invocation.
	 */
	@NonNull
	@Override
	<T> Mono<T> invokeCallback(EntityCallback<T> callback, @Nullable T entity,
			BiFunction<EntityCallback<T>, T, Object> callbackInvokerFunction);
}
