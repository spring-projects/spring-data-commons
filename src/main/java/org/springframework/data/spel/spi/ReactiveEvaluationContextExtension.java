/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.spel.spi;

import reactor.core.publisher.Mono;

import org.springframework.expression.EvaluationContext;

/**
 * SPI to resolve a {@link EvaluationContextExtension} to make it accessible via the root of an
 * {@link EvaluationContext} provided by a
 * {@link org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider}.
 * <p>
 * Extensions can be ordered by following Spring's {@link org.springframework.core.Ordered} conventions.
 *
 * @author Mark Paluch
 * @since 2.4
 * @see EvaluationContextExtension
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 */
public interface ReactiveEvaluationContextExtension extends ExtensionIdAware {

	/**
	 * Return the {@link EvaluationContextExtension} to be consumed during the actual execution. It's strongly recommended
	 * to declare the most concrete type possible as return type of the implementation method. This will allow us to
	 * obtain the necessary metadata once and not for every evaluation.
	 *
	 * @return the resolved {@link EvaluationContextExtension}. Publishers emitting no value will be skipped.
	 */
	Mono<? extends EvaluationContextExtension> getExtension();
}
