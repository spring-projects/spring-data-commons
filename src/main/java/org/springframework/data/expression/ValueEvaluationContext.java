/*
 * Copyright 2024 the original author or authors.
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
import org.springframework.lang.Nullable;

/**
 * Expressions are executed in an evaluation context. It is in this context that references are resolved when
 * encountered during expression evaluation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.3
 */
public interface ValueEvaluationContext {

	/**
	 * Returns a new {@link ValueEvaluationContext}.
	 *
	 * @param environment
	 * @param evaluationContext
	 * @return a new {@link ValueEvaluationContext} for the given environment and evaluation context.
	 */
	static ValueEvaluationContext of(@Nullable Environment environment, EvaluationContext evaluationContext) {
		return new DefaultValueEvaluationContext(environment, evaluationContext);
	}

	/**
	 * Returns the {@link Environment} if provided.
	 *
	 * @return the {@link Environment} or {@literal null}.
	 */
	@Nullable
	Environment getEnvironment();

	/**
	 * Returns the {@link EvaluationContext} if provided.
	 *
	 * @return the {@link EvaluationContext} or {@literal null} if not set.
	 */
	@Nullable
	EvaluationContext getEvaluationContext();

	/**
	 * Returns the required {@link EvaluationContext} or throws {@link IllegalStateException} if there is no evaluation
	 * context available.
	 *
	 * @return the {@link EvaluationContext}.
	 * @since 3.4
	 */
	default EvaluationContext getRequiredEvaluationContext() {

		EvaluationContext evaluationContext = getEvaluationContext();

		if (evaluationContext == null) {
			throw new IllegalStateException("No evaluation context available");
		}

		return evaluationContext;
	}
}
