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

import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationException;

/**
 * Property placeholder expression evaluated against a {@link Environment}.
 *
 * @param expression
 * @author Mark Paluch
 * @since 3.3
 */
record PlaceholderExpression(String expression) implements ValueExpression {

	@Override
	public String getExpressionString() {
		return expression;
	}

	@Override
	public boolean isLiteral() {
		return false;
	}

	@Override
	@SuppressWarnings("NullAway")
	public String evaluate(ValueEvaluationContext context) {

		Environment environment = context.getEnvironment();
		if (environment != null) {
			try {
				return environment.resolveRequiredPlaceholders(expression);
			} catch (IllegalArgumentException e) {
				throw new EvaluationException(e.getMessage(), e);
			}
		}
		return expression;
	}

	@Override
	public Class<?> getValueType(ValueEvaluationContext context) {
		return String.class;
	}

}
