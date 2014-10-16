/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue.core.spel;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ClassUtils;

/**
 * Factory capable of parsing raw expression strings taking Spring 4.1 compiled expressions into concern. Will fall back
 * to non compiled ones in case lower than 4.1 Spring version is detected or expression compilation fails.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class SpelExpressionFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpelExpressionFactory.class);

	private static final boolean IS_SPEL_COMPILER_PRESENT = ClassUtils.isPresent(
			"org.springframework.expression.spel.standard.SpelCompiler", SpelExpressionFactory.class.getClassLoader());

	private static final SpelParserConfiguration DEFAULT_PARSER_CONFIG = new SpelParserConfiguration(false, false);

	private static SpelExpressionParser compiledModeExpressionParser;
	private static SpelExpressionParser expressionParser;

	static {

		expressionParser = new SpelExpressionParser(DEFAULT_PARSER_CONFIG);

		if (IS_SPEL_COMPILER_PRESENT) {
			compiledModeExpressionParser = new SpelExpressionParser(silentlyInitializeCompiledMode("IMMEDIATE"));
		}
	}

	/**
	 * @param expressionString
	 * @return
	 */
	public static SpelExpression parseRaw(String expressionString) {

		if (compiledModeExpressionParser != null) {
			try {
				return compiledModeExpressionParser.parseRaw(expressionString);
			} catch (ExpressionException ex) {
				LOGGER.info(String.format("Could parse expression %s in compiked mode. Using fallback.", expressionString), ex);
			}
		}

		return expressionParser.parseRaw(expressionString);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static SpelParserConfiguration silentlyInitializeCompiledMode(String mode) {

		try {
			Class compilerMode = ClassUtils.forName("org.springframework.expression.spel.SpelCompilerMode",
					SpelExpressionFactory.class.getClassLoader());

			Constructor<SpelParserConfiguration> constructor = ClassUtils.getConstructorIfAvailable(
					SpelParserConfiguration.class, compilerMode, ClassLoader.class);
			if (constructor != null) {
				return BeanUtils.instantiateClass(constructor, Enum.valueOf(compilerMode, mode.toUpperCase()),
						SpelExpressionFactory.class.getClassLoader());
			}
		} catch (Exception e) {
			LOGGER.info(String.format("Could not create SpelParserConfiguration for mode '%s'.", mode), e);
		}

		return DEFAULT_PARSER_CONFIG;
	}

}
