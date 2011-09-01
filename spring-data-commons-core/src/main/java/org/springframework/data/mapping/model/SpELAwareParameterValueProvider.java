/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * {@link ParameterValueProvider} implementation that evaluates the {@link Parameter}s key against
 * {@link SpelExpressionParser} and {@link EvaluationContext}.
 * 
 * @author Oliver Gierke
 */
public class SpELAwareParameterValueProvider implements ParameterValueProvider {

	private final SpelExpressionParser parser;
	private final EvaluationContext context;

	/**
	 * Creates a new {@link SpELAwareParameterValueProvider} from the given {@link SpelExpressionParser} and
	 * {@link EvaluationContext}.
	 * 
	 * @param parser must not be {@literal null}
	 * @param context must not be {@literal null}
	 */
	public SpELAwareParameterValueProvider(SpelExpressionParser parser, EvaluationContext context) {
		Assert.notNull(parser);
		Assert.notNull(context);
		this.parser = parser;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.model.PreferredConstructor.ParameterValueProvider#getParameterValue(org.springframework.data.mapping.model.PreferredConstructor.Parameter)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getParameterValue(Parameter<T> parameter) {
		Expression expression = parser.parseExpression(parameter.getKey());
		return (T) expression.getValue(context);
	}
}
