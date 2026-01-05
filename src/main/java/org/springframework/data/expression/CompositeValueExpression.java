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

import java.util.List;

import org.springframework.data.spel.ExpressionDependencies;

/**
 * Composite {@link ValueExpression} consisting of multiple placeholder, SpEL, and literal expressions.
 *
 * @param raw
 * @param expressions
 * @author Mark Paluch
 * @since 3.3
 */
record CompositeValueExpression(String raw, List<ValueExpression> expressions) implements ValueExpression {

	@Override
	public String getExpressionString() {
		return raw;
	}

	@Override
	public ExpressionDependencies getExpressionDependencies() {

		ExpressionDependencies dependencies = ExpressionDependencies.none();

		for (ValueExpression expression : expressions) {
			ExpressionDependencies dependency = expression.getExpressionDependencies();
			if (!dependency.equals(ExpressionDependencies.none())) {
				dependencies = dependencies.mergeWith(dependency);
			}
		}

		return dependencies;
	}

	@Override
	public boolean isLiteral() {

		for (ValueExpression expression : expressions) {
			if (!expression.isLiteral()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String evaluate(ValueEvaluationContext context) {

		StringBuilder builder = new StringBuilder();

		for (ValueExpression expression : expressions) {
			builder.append((String) expression.evaluate(context));
		}

		return builder.toString();
	}

	@Override
	public Class<?> getValueType(ValueEvaluationContext context) {
		return String.class;
	}

}
