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

import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.util.ConcurrentLruCache;

/**
 * Caching variant of {@link ValueExpressionDelegate}.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class CachingValueExpressionDelegate extends ValueExpressionDelegate {

	private final ConcurrentLruCache<String, ValueExpression> expressionCache;

	/**
	 * Creates a new {@link CachingValueExpressionDelegate} given {@link ValueExpressionDelegate}.
	 *
	 * @param delegate must not be {@literal null}.
	 */
	public CachingValueExpressionDelegate(ValueExpressionDelegate delegate) {
		super(delegate);
		this.expressionCache = new ConcurrentLruCache<>(256, delegate.getValueExpressionParser()::parse);
	}

	/**
	 * Creates a new {@link CachingValueExpressionDelegate} given {@link QueryMethodValueEvaluationContextAccessor} and
	 * {@link ValueExpressionParser}.
	 *
	 * @param providerFactory the factory to create value evaluation context providers, must not be {@code null}.
	 * @param valueExpressionParser the parser to parse expression strings into value expressions, must not be
	 *          {@code null}.
	 */
	public CachingValueExpressionDelegate(QueryMethodValueEvaluationContextAccessor providerFactory,
			ValueExpressionParser valueExpressionParser) {
		super(providerFactory, valueExpressionParser);
		this.expressionCache = new ConcurrentLruCache<>(256, valueExpressionParser::parse);
	}

	@Override
	public ValueExpressionParser getValueExpressionParser() {
		return this;
	}

	@Override
	public ValueExpression parse(String expressionString) throws ParseException {
		return expressionCache.get(expressionString);
	}
}
