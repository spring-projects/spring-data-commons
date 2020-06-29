/*
 * Copyright 2014-2020 the original author or authors.
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
package org.springframework.data.spel;

import reactor.core.publisher.Mono;

import org.springframework.expression.EvaluationContext;

/**
 * Provides a way to access a centrally defined potentially shared {@link EvaluationContext}.
 *
 * @author Mark Paluch
 * @since 2.4
 */
@FunctionalInterface
public interface ReactiveEvaluationContextProvider {

	/**
	 * Returns an {@link EvaluationContext} built using the given parameter values.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @return mono emitting the {@link EvaluationContext}.
	 */
	Mono<? extends EvaluationContext> getEvaluationContext(Object rootObject);

	/**
	 * Returns a tailored {@link EvaluationContext} built using the given parameter values and considering
	 * {@link ExpressionDependencies.ExpressionDependency expression dependencies}. The returned {@link EvaluationContext}
	 * may contain a reduced visibility of methods and properties/fields according to the required
	 * {@link ExpressionDependencies.ExpressionDependency expression dependencies}.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @param dependencies the requested expression dependencies to be available.
	 * @return mono emitting the {@link EvaluationContext}.
	 * @since 2.4
	 */
	default Mono<? extends EvaluationContext> getEvaluationContext(Object rootObject,
			ExpressionDependencies dependencies) {
		return getEvaluationContext(rootObject);
	}
}
