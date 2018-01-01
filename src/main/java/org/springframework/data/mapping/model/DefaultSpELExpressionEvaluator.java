/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping.model;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

/**
 * {@link ParameterValueProvider} implementation that evaluates the {@link Parameter}s key against
 * {@link SpelExpressionParser} and {@link EvaluationContext}.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DefaultSpELExpressionEvaluator implements SpELExpressionEvaluator {

	private final @NonNull Object source;
	private final @NonNull SpELContext factory;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.SpELExpressionEvaluator#evaluate(java.lang.String)
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T evaluate(String expression) {

		Expression parseExpression = factory.getParser().parseExpression(expression);
		return (T) parseExpression.getValue(factory.getEvaluationContext(source));
	}
}
