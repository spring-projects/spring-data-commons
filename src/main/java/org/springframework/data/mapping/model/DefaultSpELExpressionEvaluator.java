/*
 * Copyright 2011-2024 the original author or authors.
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

package org.springframework.data.mapping.model;

import org.springframework.data.mapping.Parameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ParameterValueProvider} implementation that evaluates the {@link Parameter}s key against
 * {@link SpelExpressionParser} and {@link EvaluationContext}.
 *
 * @author Oliver Gierke
 * @deprecated since 3.3, use {@link CachingValueExpressionEvaluatorFactory} instead.
 */
@Deprecated(since = "3.3")
public class DefaultSpELExpressionEvaluator implements SpELExpressionEvaluator {

	private final Object source;
	private final SpELContext factory;

	public DefaultSpELExpressionEvaluator(Object source, SpELContext factory) {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(factory, "SpELContext must not be null");

		this.source = source;
		this.factory = factory;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T evaluate(String expression) {

		Expression parseExpression = factory.getParser().parseExpression(expression);
		return (T) parseExpression.getValue(factory.getEvaluationContext(source));
	}
}
