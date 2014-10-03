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
package org.springframework.data.util;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.repository.inmemory.map.SpelSort;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public abstract class SpelUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpelUtil.class);

	private static final boolean IS_SPEL_COMPILER_PRESENT = ClassUtils.isPresent(
			"org.springframework.expression.spel.standard.SpelCompiler", SpelSort.class.getClassLoader());

	private static final SpelParserConfiguration DEFAULT_PARSER_CONFIG = new SpelParserConfiguration(false, false);

	public static SpelParserConfiguration silentlyCreateParserConfiguration(String mode) {

		if (!IS_SPEL_COMPILER_PRESENT) {
			return DEFAULT_PARSER_CONFIG;
		}

		return silentlyInitializeCompiledMode(StringUtils.hasText(mode) ? mode : "OFF");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static SpelParserConfiguration silentlyInitializeCompiledMode(String mode) {

		try {
			Class compilerMode = ClassUtils.forName("org.springframework.expression.spel.SpelCompilerMode",
					SpelUtil.class.getClassLoader());

			Constructor<SpelParserConfiguration> constructor = ClassUtils.getConstructorIfAvailable(
					SpelParserConfiguration.class, compilerMode, ClassLoader.class);
			if (constructor != null) {
				return BeanUtils.instantiateClass(constructor, Enum.valueOf(compilerMode, mode.toUpperCase()),
						SpelUtil.class.getClassLoader());
			}
		} catch (Exception e) {
			LOGGER.info(String.format("Could not create SpelParserConfiguration for mode '%s'.", mode), e);
		}

		return DEFAULT_PARSER_CONFIG;
	}

}
