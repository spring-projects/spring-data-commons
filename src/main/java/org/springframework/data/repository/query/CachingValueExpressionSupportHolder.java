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
package org.springframework.data.repository.query;

import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.util.ConcurrentLruCache;

/**
 * Caching variant of {@link ValueExpressionSupportHolder}.
 *
 * @author Mark Paluch
 */
public class CachingValueExpressionSupportHolder extends ValueExpressionSupportHolder {

	private final ConcurrentLruCache<String, ValueExpression> expressionCache;

	public CachingValueExpressionSupportHolder(ValueExpressionSupportHolder supportHolder) {
		super(supportHolder);
		this.expressionCache = new ConcurrentLruCache<>(256, supportHolder.getValueExpressionParser()::parse);
	}

	public CachingValueExpressionSupportHolder(QueryMethodValueEvaluationContextProviderFactory providerFactory,
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
