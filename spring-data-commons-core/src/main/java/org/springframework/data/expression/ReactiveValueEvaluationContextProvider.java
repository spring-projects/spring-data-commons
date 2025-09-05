/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.expression;

import reactor.core.publisher.Mono;

import org.jspecify.annotations.Nullable;

import org.springframework.data.spel.ExpressionDependencies;

/**
 * Reactive extension to {@link ValueEvaluationContext} for obtaining a {@link ValueEvaluationContext} that participates
 * in the reactive flow.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface ReactiveValueEvaluationContextProvider extends ValueEvaluationContextProvider {

	/**
	 * Return a {@link ValueEvaluationContext} built using the given parameter values.
	 *
	 * @param rootObject the root object to set in the {@link ValueEvaluationContext}.
	 * @return a mono that emits exactly one {@link ValueEvaluationContext}.
	 */
	Mono<ValueEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject);

	/**
	 * Return a tailored {@link ValueEvaluationContext} built using the given parameter values and considering
	 * {@link ExpressionDependencies expression dependencies}. The returned {@link ValueEvaluationContext} may contain a
	 * reduced visibility of methods and properties/fields according to the required {@link ExpressionDependencies
	 * expression dependencies}.
	 *
	 * @param rootObject the root object to set in the {@link ValueEvaluationContext}.
	 * @param dependencies the requested expression dependencies to be available.
	 * @return a mono that emits exactly one {@link ValueEvaluationContext}.
	 */
	default Mono<ValueEvaluationContext> getEvaluationContextLater(@Nullable Object rootObject,
			ExpressionDependencies dependencies) {
		return getEvaluationContextLater(rootObject);
	}
}
