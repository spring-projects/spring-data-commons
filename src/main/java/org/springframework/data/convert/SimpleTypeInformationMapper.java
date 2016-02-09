/*
 * Copyright 2011-2016 the original author or authors.
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
package org.springframework.data.convert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Basic {@link TypeInformationMapper} implementation that interprets the alias handles as fully qualified class name
 * and tries to load a class with the given name to build {@link TypeInformation}. Returns the fully qualified class
 * name for alias creation.
 * 
 * @author Oliver Gierke
 */
public class SimpleTypeInformationMapper implements TypeInformationMapper {

	/**
	 * @deprecated prefer to instantiate directly.
	 */
	@Deprecated //
	public static final SimpleTypeInformationMapper INSTANCE = new SimpleTypeInformationMapper();

	private final Map<String, ClassTypeInformation<?>> CACHE = new ConcurrentHashMap<String, ClassTypeInformation<?>>();

	/**
	 * Returns the {@link TypeInformation} that shall be used when the given {@link String} value is found as type hint.
	 * The implementation will simply interpret the given value as fully-qualified class name and try to load the class.
	 * Will return {@literal null} in case the given {@link String} is empty.
	 * 
	 * @param value the type to load, must not be {@literal null}.
	 * @return the type to be used for the given {@link String} representation or {@literal null} if nothing found or the
	 *         class cannot be loaded.
	 */
	public ClassTypeInformation<?> resolveTypeFrom(Object alias) {

		if (!(alias instanceof String)) {
			return null;
		}

		String value = (String) alias;

		if (!StringUtils.hasText(value)) {
			return null;
		}

		ClassTypeInformation<?> information = CACHE.get(value);

		if (information != null) {
			return information;
		}

		try {
			information = ClassTypeInformation.from(ClassUtils.forName(value, null));
		} catch (ClassNotFoundException e) {
			return null;
		}

		if (information != null) {
			CACHE.put(value, information);
		}

		return information;
	}

	/**
	 * Turn the given type information into the String representation that shall be stored. Default implementation simply
	 * returns the fully-qualified class name.
	 * 
	 * @param typeInformation must not be {@literal null}.
	 * @return the String representation to be stored or {@literal null} if no type information shall be stored.
	 */
	public String createAliasFor(TypeInformation<?> type) {
		return type.getType().getName();
	}
}
