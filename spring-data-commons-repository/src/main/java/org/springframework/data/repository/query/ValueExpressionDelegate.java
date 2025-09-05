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
package org.springframework.data.repository.query;

import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.expression.ParseException;

/**
 * Delegate to provide a {@link ValueExpressionParser} along with a context factory.
 * <p>
 * Subclasses can customize parsing behavior.
 *
 * @author Mark Paluch
 */
public class ValueExpressionDelegate implements ValueExpressionParser {

	private final QueryMethodValueEvaluationContextAccessor contextAccessor;
	private final ValueExpressionParser valueExpressionParser;

	/**
	 * Creates a new {@link ValueExpressionDelegate} given {@link QueryMethodValueEvaluationContextAccessor} and
	 * {@link ValueExpressionParser}.
	 *
	 * @param contextAccessor the factory to create value evaluation context providers, must not be {@code null}.
	 * @param valueExpressionParser the parser to parse expression strings into value expressions, must not be
	 *          {@code null}.
	 */
	public ValueExpressionDelegate(QueryMethodValueEvaluationContextAccessor contextAccessor,
			ValueExpressionParser valueExpressionParser) {
		this.contextAccessor = contextAccessor;
		this.valueExpressionParser = valueExpressionParser;
	}

	ValueExpressionDelegate(ValueExpressionDelegate original) {
		this.contextAccessor = original.contextAccessor;
		this.valueExpressionParser = original.valueExpressionParser;
	}

	/**
	 * Creates a default {@link ValueExpressionDelegate} using the
	 * {@link org.springframework.core.env.StandardEnvironment}, a default {@link ValueExpression} and extension-less
	 * {@link org.springframework.data.spel.EvaluationContextProvider}.
	 *
	 * @return a default {@link ValueExpressionDelegate}.
	 */
	public static ValueExpressionDelegate create() {
		return new ValueExpressionDelegate(QueryMethodValueEvaluationContextAccessor.create(),
				ValueExpressionParser.create());
	}

	public ValueExpressionParser getValueExpressionParser() {
		return valueExpressionParser;
	}

	public QueryMethodValueEvaluationContextAccessor getEvaluationContextAccessor() {
		return contextAccessor;
	}

	/**
	 * Creates a {@link ValueEvaluationContextProvider} for query method {@link Parameters} for later creation of a
	 * {@link ValueEvaluationContext} based on the actual method parameter values. The resulting
	 * {@link ValueEvaluationContextProvider} is only valid for the given parameters
	 *
	 * @param parameters the query method parameters to use.
	 * @return
	 */
	public ValueEvaluationContextProvider createValueContextProvider(Parameters<?, ?> parameters) {
		return contextAccessor.create(parameters);
	}

	@Override
	public ValueExpression parse(String expressionString) throws ParseException {
		return valueExpressionParser.parse(expressionString);
	}
}
