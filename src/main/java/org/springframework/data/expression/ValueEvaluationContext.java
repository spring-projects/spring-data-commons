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

import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;

/**
 * Expressions are executed using an evaluation context. This context is used to resolve references during (SpEL,
 * property placeholder) expression evaluation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.3
 */
public interface ValueEvaluationContext {

	/**
	 * Returns a new {@link ValueEvaluationContext}.
	 *
	 * @param environment must not be {@literal null}.
	 * @param evaluationContext must not be {@literal null}.
	 * @return a new {@link ValueEvaluationContext} for the given environment and evaluation context.
	 */
	static ValueEvaluationContext of(Environment environment, EvaluationContext evaluationContext) {
		return new DefaultValueEvaluationContext(environment, evaluationContext);
	}

	/**
	 * Returns the {@link Environment}.
	 *
	 * @return the {@link Environment}.
	 */
	Environment getEnvironment();

	/**
	 * Returns the {@link EvaluationContext}.
	 *
	 * @return the {@link EvaluationContext}.
	 */
	EvaluationContext getEvaluationContext();

	/**
	 * Returns the required {@link EvaluationContext} or throws {@link IllegalStateException} if there is no evaluation
	 * context available.
	 *
	 * @return the {@link EvaluationContext}.
	 * @since 3.4
	 * @deprecated since 4.0, EvaluationContext is always provided.
	 */
	@Deprecated
	@SuppressWarnings("ConstantValue")
	default EvaluationContext getRequiredEvaluationContext() {

		EvaluationContext evaluationContext = getEvaluationContext();

		if (evaluationContext == null) {
			throw new IllegalStateException("No evaluation context available");
		}

		return evaluationContext;
	}
}
