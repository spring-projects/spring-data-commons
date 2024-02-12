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

import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

/**
 * SpEL expression.
 *
 * @param expression
 * @author Mark Paluch
 * @since 3.3
 */
record ExpressionExpression(Expression expression, ExpressionDependencies dependencies) implements ValueExpression {

	@Override
	public String getExpressionString() {
		return expression.getExpressionString();
	}

	@Override
	public ExpressionDependencies getExpressionDependencies() {
		return dependencies();
	}

	@Override
	public boolean isLiteral() {
		return false;
	}

	@Override
	public Object evaluate(ValueEvaluationContext context) {

		EvaluationContext evaluationContext = context.getEvaluationContext();
		if (evaluationContext != null) {
			return expression.getValue(evaluationContext);
		}
		return expression.getValue();
	}
}
