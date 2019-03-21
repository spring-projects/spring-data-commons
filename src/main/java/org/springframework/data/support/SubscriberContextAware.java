/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.support;

import reactor.util.context.Context;

import org.springframework.beans.factory.Aware;

/**
 * Interface to be implemented by objects that wish to be aware of the current subscriber {@link Context} during
 * materialization of a reactive sequence.
 * <p/>
 * For example, {@link org.springframework.data.spel.spi.EvaluationContextExtension}s that want to contribute properties
 * based on contextual values to be evaluated during repository query method execution.
 * <p/>
 * Objects that implement this interface should be immutable and create a new instance upon
 * {@link #withSubscriberContext(Context)} to act only within the context that is valid only within an individual
 * subscription.
 *
 * @author Mark Paluch
 * @since 2.1
 * @see Context
 * @see reactor.core.publisher.Mono#subscriberContext()
 */
public interface SubscriberContextAware<T> extends Aware {

	/**
	 * Callback that supplies the associated subscriber {@link Context} to an extension object.
	 * <p>
	 * Invoked after initialization and during repository query method parameter evaluation.
	 *
	 * @param context the subscriber context, never {@code null}. May be {@link Context#empty()} if no context is
	 *          associated/the context is empty.
	 * @return a new {@link org.springframework.data.spel.spi.EvaluationContextExtension} instance that is only valid
	 *         during the lifespan of the enclosing subscription.
	 */
	T withSubscriberContext(Context context);
}
