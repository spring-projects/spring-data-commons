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
package org.springframework.data.mapping.model;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.EnvironmentCapable;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.ExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * Factory to create a ValueExpressionEvaluator
 *
 * @author Mark Paluch
 * @since 3.3
 */
public class CachingValueExpressionEvaluatorFactory implements ValueEvaluationContextProvider {

	private final ConcurrentLruCache<String, ValueExpression> expressionCache;
	private final EnvironmentCapable environmentProvider;
	private final EvaluationContextProvider evaluationContextProvider;

	/**
	 * Creates a new {@link CachingValueExpressionEvaluatorFactory} for the given {@link ExpressionParser},
	 * {@link EnvironmentCapable Environment provider} and {@link EvaluationContextProvider} with a cache size of 256.
	 *
	 * @param expressionParser
	 * @param environmentProvider
	 * @param evaluationContextProvider
	 */
	public CachingValueExpressionEvaluatorFactory(ExpressionParser expressionParser,
			EnvironmentCapable environmentProvider, EvaluationContextProvider evaluationContextProvider) {
		this(expressionParser, environmentProvider, evaluationContextProvider, 256);
	}

	/**
	 * Creates a new {@link CachingValueExpressionEvaluatorFactory} for the given {@link ExpressionParser},
	 * {@link EnvironmentCapable Environment provider} and {@link EvaluationContextProvider} with a specific
	 * {@code cacheSize}.
	 *
	 * @param expressionParser
	 * @param environmentProvider
	 * @param evaluationContextProvider
	 * @param cacheSize
	 */
	public CachingValueExpressionEvaluatorFactory(ExpressionParser expressionParser,
			EnvironmentCapable environmentProvider, EvaluationContextProvider evaluationContextProvider, int cacheSize) {

		Assert.notNull(expressionParser, "ExpressionParser must not be null");

		ValueExpressionParser parser = ValueExpressionParser.create(() -> expressionParser);
		this.expressionCache = new ConcurrentLruCache<>(cacheSize, parser::parse);
		this.environmentProvider = environmentProvider;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	@Override
	public ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject) {
		return ValueEvaluationContext.of(environmentProvider.getEnvironment(),
				evaluationContextProvider.getEvaluationContext(rootObject));
	}

	@Override
	public ValueEvaluationContext getEvaluationContext(@Nullable Object rootObject, ExpressionDependencies dependencies) {
		return ValueEvaluationContext.of(environmentProvider.getEnvironment(),
				evaluationContextProvider.getEvaluationContext(rootObject, dependencies));
	}

	/**
	 * Creates a new {@link ValueExpressionEvaluator} using the given {@code source} as root object.
	 *
	 * @param source the root object for evaluating the expression.
	 * @return a new {@link ValueExpressionEvaluator} to evaluate the expression in the context of the given
	 *         {@code source} object.
	 */
	public ValueExpressionEvaluator create(Object source) {

		return new ValueExpressionEvaluator() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> @Nullable T evaluate(String expression) {
				ValueExpression valueExpression = expressionCache.get(expression);
				return (T) valueExpression.evaluate(getEvaluationContext(source, valueExpression.getExpressionDependencies()));
			}
		};
	}

}
