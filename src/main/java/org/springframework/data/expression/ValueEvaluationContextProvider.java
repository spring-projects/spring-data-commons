/*
 * Copyright 2024-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.EvaluationContext;

/**
 * SPI to provide to access a centrally defined potentially shared {@link ValueEvaluationContext}.
 *
 * @author Mark Paluch
 * @since 3.3
 */
@FunctionalInterface
public interface ValueEvaluationContextProvider {

	/**
	 * Return a {@link EvaluationContext} built using the given parameter values.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @return
	 */
	ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject);

	/**
	 * Return a tailored {@link EvaluationContext} built using the given parameter values and considering
	 * {@link ExpressionDependencies expression dependencies}. The returned {@link EvaluationContext} may contain a
	 * reduced visibility of methods and properties/fields according to the required {@link ExpressionDependencies
	 * expression dependencies}.
	 *
	 * @param rootObject the root object to set in the {@link EvaluationContext}.
	 * @param dependencies the requested expression dependencies to be available.
	 * @return
	 */
	default ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject,
			ExpressionDependencies dependencies) {
		return getEvaluationContext(rootObject);
	}
}
