/*
 * Copyright 2014-2024 the original author or authors.
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
package org.springframework.data.repository.query;

import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.spel.ExpressionDependencies;

/**
 * Provides a way to access a centrally defined potentially shared {@link ValueEvaluationContext}.
 *
 * @author Mark Paluch
 * @since 3.3
 */
public interface QueryMethodValueEvaluationContextProvider {

	/**
	 * Returns an {@link ValueEvaluationContext} built using the given parameter values and
	 * {@link ExpressionDependencies}.
	 *
	 * @param parameterValues the values for the parameters.
	 * @param dependencies the expression dependencies.
	 * @return an {@link ValueEvaluationContext} built using the given parameter values and *
	 *         {@link ExpressionDependencies}.
	 */
	ValueEvaluationContext getEvaluationContext(Object[] parameterValues, ExpressionDependencies dependencies);

}
