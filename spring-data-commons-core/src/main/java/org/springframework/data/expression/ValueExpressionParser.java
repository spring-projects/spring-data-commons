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
package org.springframework.data.expression;

import org.springframework.expression.ParseException;

/**
 * Parses expression strings into expressions that can be evaluated. Supports parsing expression, configuration
 * templates as well as literal strings.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.3
 */
public interface ValueExpressionParser {

	/**
	 * Creates a default parser to parse expression strings.
	 *
	 * @return the parser instance.
	 * @since 3.4
	 */
	static ValueExpressionParser create() {
		return DefaultValueExpressionParser.DEFAULT;
	}

	/**
	 * Creates a new parser to parse expression strings.
	 *
	 * @param configuration the parser context configuration.
	 * @return the parser instance.
	 */
	static ValueExpressionParser create(ValueParserConfiguration configuration) {
		return new DefaultValueExpressionParser(configuration);
	}

	/**
	 * Parses the expression string and return an Expression object you can use for repeated evaluation.
	 * <p>
	 * Some examples:
	 *
	 * <pre class="code">
	 *     #{3 + 4}
	 *     #{name.firstName}
	 *     ${key.one}
	 *     #{name.lastName}-${key.one}
	 * </pre>
	 *
	 * @param expressionString the raw expression string to parse.
	 * @return an evaluator for the parsed expression.
	 * @throws ParseException an exception occurred during parsing.
	 */
	ValueExpression parse(String expressionString) throws ParseException;

}
