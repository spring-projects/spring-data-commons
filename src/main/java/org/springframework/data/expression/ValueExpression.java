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
import org.springframework.expression.EvaluationException;

/**
 * An expression capable of evaluating itself against context objects. Encapsulates the details of a previously parsed
 * expression string. Provides a common abstraction for expression evaluation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.3
 */
public interface ValueExpression {

	/**
	 * Returns the original string used to create this expression (unmodified).
	 *
	 * @return the original expression string.
	 */
	String getExpressionString();

	/**
	 * Returns the expression dependencies.
	 *
	 * @return the dependencies the underlying expression requires. Can be {@link ExpressionDependencies#none()}.
	 */
	default ExpressionDependencies getExpressionDependencies() {
		return ExpressionDependencies.none();
	}

	/**
	 * Returns whether the expression is a literal expression (that doesn't actually require evaluation).
	 *
	 * @return {@code true} if the expression is a literal expression; {@code false} if the expression can yield a
	 *         different result upon {@link #evaluate(ValueEvaluationContext) evaluation}.
	 */
	boolean isLiteral();

	/**
	 * Evaluates this expression using the given evaluation context.
	 *
	 * @return the evaluation result.
	 * @throws EvaluationException if there is a problem during evaluation
	 */
	@Nullable
	Object evaluate(ValueEvaluationContext context) throws EvaluationException;

	/**
	 * Return the most general type that the expression would use as return type for the given context.
	 *
	 * @param context the context in which to evaluate the expression.
	 * @return the most general type of value.
	 * @throws EvaluationException if there is a problem determining the type
	 * @since 3.4
	 */
	@Nullable
	Class<?> getValueType(ValueEvaluationContext context) throws EvaluationException;

}
