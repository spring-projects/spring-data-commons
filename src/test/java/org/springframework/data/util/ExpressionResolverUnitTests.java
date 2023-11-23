/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.util;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.mapping.context.AbstractMappingContext.DelegatingEnvironmentAccessor;
import org.springframework.data.support.EnvironmentAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2023/11
 */
public class ExpressionResolverUnitTests {

	@Test
	void xxx() {

		ParserContext parserContext = new DefaultParserContext(new DelegatingEnvironmentAccessor(new StandardEnvironment()), new SpelExpressionParser());

		ValueParser parser = ValueParser.create(parserContext);

		ParsableStatement statement = parser.prepareStatement("foo-${os.arch}");
//		PreparedStatement bindNow = parser.parse("foo-${os.arch}");

		statement.evaluate(ValueContext.spelContext(new StandardEvaluationContext()));
	}


	interface ValueParser {
		ParsableStatement prepareStatement(String expression);
		static ValueParser create(ParserContext parserContext) {
			return new DefaultValueParser(parserContext);
		}
	}

	static class DefaultValueParser implements ValueParser {

		private final ParserContext parserContext;

		public DefaultValueParser(ParserContext parserContext) {
			this.parserContext = parserContext;
		}

		@Override
		public ParsableStatement prepareStatement(String expression) {
			return new RawStatement(expression, parserContext);
		}
	}

	interface ParsableStatement {

		<T> T evaluate(ValueContext context);

		default ParsableStatement transform(Function<ParsableStatement, ParsableStatement> mappingFunction) {
			return mappingFunction.apply(this);
		}

		String toString();
	}

	class LazyEvaluatingStatement {

	}

	abstract static class AbstractStatement implements ParsableStatement {

		final ParserContext parserContext;

		public AbstractStatement(ParserContext parserContext) {
			this.parserContext = parserContext;
		}

		@Override
		public <T> T evaluate(ValueContext context) {

			ParsableStatement executableStatement = transform(parserContext::resolvePlaceholders)
					.transform(parserContext::parseExpressions);

			return executableStatement.evaluate(context);
		}

		abstract String getValue();
	}

	static class RawStatement extends AbstractStatement {

		final String value;

		public RawStatement(String value, ParserContext context) {
			super(context);
			this.value = value;
		}

		@Override
		String getValue() {
			return value;
		}
	}

	static class PreparedStatement extends RawStatement {

		public PreparedStatement(String value, ParserContext context) {
			super(value, context);
		}
	}

	static class ExpressionStatement implements ParsableStatement {

		Expression expression;

		public ExpressionStatement(Expression expression) {
			this.expression = expression;
		}

		@Override
		public <T> T evaluate(ValueContext context) {

			if(context instanceof SpELValueContext spelContext) {
				return (T) expression.getValue(spelContext.getEvaluationContext());
			}
			return (T) expression.getValue();
		}

		@Override
		public String toString() {
			return expression.getExpressionString();
		}
	}

	interface ParserContext {

		ParsableStatement resolvePlaceholders(ParsableStatement statement);
		ParsableStatement parseExpressions(ParsableStatement statement);
	}

	static class DefaultParserContext implements ParserContext {

		private final EnvironmentAccessor environmentAccessor;
		private final SpelExpressionParser spelExpressionParser;

		public DefaultParserContext(EnvironmentAccessor environmentAccessor, SpelExpressionParser spelExpressionParser) {

			this.environmentAccessor = environmentAccessor;
			this.spelExpressionParser = spelExpressionParser;
		}

		@Override
		public ParsableStatement resolvePlaceholders(ParsableStatement statement) {
			return statement.transform(it -> {
				if (it instanceof PreparedStatement) {
					return it;
				}
				return new PreparedStatement(environmentAccessor.resolvePlaceholders(it.toString()), this);
			});
		}

		@Override
		public ParsableStatement parseExpressions(ParsableStatement statement) {
			return statement.transform(it -> {
				Expression expression = detectExpression(spelExpressionParser, it.toString());
				if (expression == null || expression instanceof LiteralExpression) {
					return it;
				}
				return new ExpressionStatement(expression);
			});
		}

		@Nullable
		private static Expression detectExpression(SpelExpressionParser parser, @Nullable String potentialExpression) {

			if (!StringUtils.hasText(potentialExpression)) {
				return null;
			}

			Expression expression = parser.parseExpression(potentialExpression, org.springframework.expression.ParserContext.TEMPLATE_EXPRESSION);
			return expression instanceof LiteralExpression ? null : expression;
		}
	}

	sealed interface ValueContext permits SpELValueContext {

		static ValueContext spelContext(EvaluationContext evaluationContext) {
			return new SpELValueContext(evaluationContext);
		}
	}

	static final class SpELValueContext implements ValueContext {
		EvaluationContext evaluationContext;

		public SpELValueContext(EvaluationContext evaluationContext) {
			this.evaluationContext = evaluationContext;
		}

		EvaluationContext getEvaluationContext() {
			return evaluationContext;
		}
	}
}
