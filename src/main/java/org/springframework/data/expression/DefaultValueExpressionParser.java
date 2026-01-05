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

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

/**
 * Default {@link ValueExpressionParser} implementation. Instances are thread-safe.
 *
 * @author Mark Paluch
 * @since 3.3
 */
class DefaultValueExpressionParser implements ValueExpressionParser {

	public static final String PLACEHOLDER_PREFIX = SystemPropertyUtils.PLACEHOLDER_PREFIX;
	public static final String EXPRESSION_PREFIX = ParserContext.TEMPLATE_EXPRESSION.getExpressionPrefix();
	public static final char SUFFIX = '}';
	public static final int PLACEHOLDER_PREFIX_LENGTH = PLACEHOLDER_PREFIX.length();
	public static final char[] QUOTE_CHARS = { '\'', '"' };

	public static final ValueExpressionParser DEFAULT = new DefaultValueExpressionParser(SpelExpressionParser::new);

	private final ValueParserConfiguration configuration;

	public DefaultValueExpressionParser(ValueParserConfiguration configuration) {

		Assert.notNull(configuration, "ValueParserConfiguration must not be null");

		this.configuration = configuration;
	}

	@Override
	public ValueExpression parse(String expressionString) {

		int placerholderIndex = expressionString.indexOf(PLACEHOLDER_PREFIX);
		int expressionIndex = expressionString.indexOf(EXPRESSION_PREFIX);

		if (placerholderIndex == -1 && expressionIndex == -1) {
			return new LiteralValueExpression(expressionString);
		}

		if (placerholderIndex != -1 && expressionIndex == -1
				&& findPlaceholderEndIndex(expressionString, placerholderIndex) != expressionString.length()) {
			return createPlaceholder(expressionString);
		}

		if (placerholderIndex == -1
				&& findPlaceholderEndIndex(expressionString, expressionIndex) != expressionString.length()) {
			return createExpression(expressionString);
		}

		return parseComposite(expressionString, placerholderIndex, expressionIndex);
	}

	private CompositeValueExpression parseComposite(String expressionString, int placerholderIndex, int expressionIndex) {

		List<ValueExpression> expressions = new ArrayList<>(PLACEHOLDER_PREFIX_LENGTH);
		int startIndex = getStartIndex(placerholderIndex, expressionIndex);

		if (startIndex != 0) {
			expressions.add(new LiteralValueExpression(expressionString.substring(0, startIndex)));
		}

		while (startIndex != -1) {

			int endIndex = findPlaceholderEndIndex(expressionString, startIndex);

			if (endIndex == -1) {
				throw new ParseException(expressionString, startIndex,
						"No ending suffix '}' for expression starting at character %d: %s".formatted(startIndex,
								expressionString.substring(startIndex)));
			}

			int afterClosingParenthesisIndex = endIndex + 1;
			String part = expressionString.substring(startIndex, afterClosingParenthesisIndex);

			if (part.startsWith(PLACEHOLDER_PREFIX)) {
				expressions.add(createPlaceholder(part));
			} else {
				expressions.add(createExpression(part));
			}

			placerholderIndex = expressionString.indexOf(PLACEHOLDER_PREFIX, endIndex);
			expressionIndex = expressionString.indexOf(EXPRESSION_PREFIX, endIndex);

			startIndex = getStartIndex(placerholderIndex, expressionIndex);

			if (startIndex == -1) {
				// no next expression but we're capturing everything after the expression as literal.
				expressions.add(new LiteralValueExpression(expressionString.substring(afterClosingParenthesisIndex)));
			} else {
				// capture literal after the expression ends and before the next starts.
				expressions
						.add(new LiteralValueExpression(expressionString.substring(afterClosingParenthesisIndex, startIndex)));
			}
		}

		return new CompositeValueExpression(expressionString, expressions);
	}

	private static int getStartIndex(int placerholderIndex, int expressionIndex) {
		return placerholderIndex != -1 && expressionIndex != -1 ? Math.min(placerholderIndex, expressionIndex)
				: placerholderIndex != -1 ? placerholderIndex : expressionIndex;
	}

	private PlaceholderExpression createPlaceholder(String part) {
		return new PlaceholderExpression(part);
	}

	private ExpressionExpression createExpression(String expression) {

		Expression expr = configuration.getExpressionParser().parseExpression(expression,
				ParserContext.TEMPLATE_EXPRESSION);
		ExpressionDependencies dependencies = ExpressionDependencies.discover(expr);
		return new ExpressionExpression(expr, dependencies);
	}

	private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {

		int index = startIndex + PLACEHOLDER_PREFIX_LENGTH;
		char quotationChar = 0;
		char nestingLevel = 0;
		boolean skipEscape = false;

		while (index < buf.length()) {

			char c = buf.charAt(index);

			if (!skipEscape && c == '\\') {
				skipEscape = true;
			} else if (skipEscape) {
				skipEscape = false;
			} else if (quotationChar == 0) {

				for (char quoteChar : QUOTE_CHARS) {
					if (quoteChar == c) {
						quotationChar = c;
						break;
					}
				}
			} else if (quotationChar == c) {
				quotationChar = 0;
			}

			if (!skipEscape && quotationChar == 0) {

				if (nestingLevel != 0 && c == SUFFIX) {
					nestingLevel--;
				} else if (c == '{') {
					nestingLevel++;
				} else if (nestingLevel == 0 && c == SUFFIX) {
					return index;
				}
			}

			index++;
		}

		return -1;
	}
}
