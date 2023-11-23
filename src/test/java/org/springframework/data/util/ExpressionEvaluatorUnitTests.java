/*
 * Copyright 2023. the original author or authors.
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

/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.mapping.context.AbstractMappingContext.DelegatingEnvironmentAccessor;
import org.springframework.data.support.EnvironmentAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2023/11
 */
public class ExpressionEvaluatorUnitTests {

	public static final SpelExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.OFF, null));

	@Test
	void intialTests() {

		ExpressionEvaluator evaluator = new ExpressionEvaluator(new DelegatingEnvironmentAccessor(new StandardEnvironment()));

		{
			Object result =

			evaluator.prepare("foo-${os.arch}")
					.parseWith(SpelExpressionParser::new)
					.withContext(StandardEvaluationContext::new)
					.evaluate();
			System.out.println("result: " + result);
		}

		{
			Object result = evaluator.prepare("#{systemProperties['os.arch']}")
					.withContext(() -> new StandardEvaluationContext(new StandardEnvironment()))
					.evaluate();
			System.out.println("result: " + result);
		}

		// @Value("#{1+1}-${os.arch}") -> 2-aarch64

		// StandardBeanExpressionResolver
		{
			Expression expression = SPEL_EXPRESSION_PARSER.parseExpression("#{systemProperties['os.arch']}-foo-${os.arch}", ParserContext.TEMPLATE_EXPRESSION);
			Object value = expression.getValue(new StandardEvaluationContext(new StandardEnvironment()));
			if(value instanceof String s) {
				//
			}

			System.out.println("value: " + value);
		}

		{
			Object result = evaluator.prepare("${os.arch}").evaluate();
			System.out.println("result: " + result);
		}

		{
			Object result = evaluator.prepare("foo-${os.arch}").evaluate();
			System.out.println("result: " + result);
		}

		{

			// check against - boot
			// plcaechoder replacement after spel
			Object result = evaluator.prepare("'#{systemProperties['os.arch']}-foo-${os.arch}'")
					.withContext(() -> new StandardEvaluationContext(new StandardEnvironment()))
					.evaluate();
			System.out.println("result: " + result);
		}

		// evaluator.prepare(source)evaluate(() -> context);

	}



	static class ExpressionEvaluator {

		private final EnvironmentAccessor environmentAccessor;

		public ExpressionEvaluator(EnvironmentAccessor environmentAccessor) {
			this.environmentAccessor = environmentAccessor;
		}

		PreparedExpression prepare(String source) {
			return this.prepare(() -> source);
		}

		PreparedExpression prepare(Supplier<String> source) {

			return new PreparedExpression(new Supplier<>() {

				@Nullable private String cachedValue;

				@Override
				public String get() {

					if (cachedValue != null) {
						return cachedValue;
					}

					String expressionSource = source.get();
					String expression = environmentAccessor.resolvePlaceholders(expressionSource);

					if (ObjectUtils.nullSafeEquals(expressionSource, expression)) {
						cachedValue = expression;
					}
					System.out.println("using: '" + expression + "' for " + expressionSource);
					return expression;
				}
			});
		}
	}

	static class PreparedExpression {

		Supplier<SpelExpressionParser> expressionParser;
		Supplier<String> source;
		Map<String, Optional<Expression>> expressionCache = new HashMap<>(1, 1F);

		public PreparedExpression(Supplier<String> source) {
			this.source = source;
			expressionParser = Lazy.of(SpelExpressionParser::new);
		}

		PreparedExpression parseWith(SpelExpressionParser expressionParser) {
			return parseWith(() -> expressionParser);
		}

		PreparedExpression parseWith(Supplier<SpelExpressionParser> expressionParser) {
			this.expressionParser = expressionParser;
			return this;
		}

		<T> T evaluate() {
			return withContext(SimpleEvaluationContext.forReadOnlyDataBinding().build()).evaluate();
		}

		ExpressionEvaluation applyReadonlyBinding() {
			return withContext(SimpleEvaluationContext.forReadOnlyDataBinding().build());
		}

		ExpressionEvaluation withContext(EvaluationContext evaluationContext) {
			return withContext(() -> evaluationContext);
		}

		ExpressionEvaluation withContext(Supplier<EvaluationContext> evaluationContext) {
			return new ExpressionEvaluation(this, evaluationContext);
		}
		<T> T doEvaluate(Supplier<EvaluationContext> evaluationContext) {

			String expressionString = source.get();
			Optional<Expression> expression = expressionCache.computeIfAbsent(expressionString,
					(String key) -> Optional.ofNullable(detectExpression(expressionParser.get(), key)));
			return (T) expression.map(it -> it.getValue(evaluationContext.get())).orElse(expressionString);
		}


		@Nullable
		private static Expression detectExpression(SpelExpressionParser parser, @Nullable String potentialExpression) {

			if (!StringUtils.hasText(potentialExpression)) {
				return null;
			}

			Expression expression = parser.parseExpression(potentialExpression, ParserContext.TEMPLATE_EXPRESSION);
			return expression instanceof LiteralExpression ? null : expression;
		}

	}

	static class ExpressionEvaluation {

		final PreparedExpression source;
		final Supplier<EvaluationContext> evaluationContext;
		boolean cacheResult;
		Optional<?> cached;

		public ExpressionEvaluation(PreparedExpression source, Supplier<EvaluationContext> evaluationContext) {
			this.source = source;
			this.evaluationContext = evaluationContext;
		}

		<T> T evaluate() {

			if(cacheResult && cached != null) {
				return (T) cached.orElse(null);
			}

			T result = source.doEvaluate(evaluationContext);
			if(cacheResult) {
				cached = Optional.ofNullable(result);
			}
			return result;
		}

		ExpressionEvaluation cache() {
			cacheResult = true;
			return this;
		}

		<T> T reevaluate() {

			if(cacheResult) {
				cached = null;
			}
			return evaluate();
		}
	}

}
