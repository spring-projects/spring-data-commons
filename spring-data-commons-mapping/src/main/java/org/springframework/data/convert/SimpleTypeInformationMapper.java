/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.convert;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mapping.Alias;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;

/**
 * Basic {@link TypeInformationMapper} implementation that interprets the alias handles as fully qualified class name
 * and tries to load a class with the given name to build {@link TypeInformation}. Returns the fully qualified class
 * name for alias creation.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class SimpleTypeInformationMapper implements TypeInformationMapper, BeanClassLoaderAware {

	private final Map<String, Optional<TypeInformation<?>>> cache = new ConcurrentHashMap<>();

	private @Nullable ClassLoader classLoader;

	/**
	 * Returns the {@link TypeInformation} that shall be used when the given {@link String} value is found as type hint.
	 * The implementation will simply interpret the given value as fully-qualified class name and try to load the class.
	 * Will return {@literal null} in case the given {@link String} is empty.
	 *
	 * @param alias the type to load, must not be {@literal null}.
	 * @return the type to be used for the given {@link String} representation or {@literal null} if nothing found or the
	 *         class cannot be loaded.
	 */
	@Override
	public @Nullable TypeInformation<?> resolveTypeFrom(Alias alias) {

		String stringAlias = alias.mapTyped(String.class);

		if (stringAlias != null) {
			return cache.computeIfAbsent(stringAlias, this::loadClass).orElse(null);
		}

		return null;
	}

	/**
	 * Turn the given type information into the String representation that shall be stored. Default implementation simply
	 * returns the fully-qualified class name.
	 *
	 * @param type must not be {@literal null}.
	 * @return the String representation to be stored or {@literal null} if no type information shall be stored.
	 */
	@Override
	public Alias createAliasFor(TypeInformation<?> type) {
		return Alias.of(type.getType().getName());
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	private Optional<TypeInformation<?>> loadClass(String typeName) {

		try {
			return Optional.of(TypeInformation.of(ClassUtils.forName(typeName, this.classLoader)));
		} catch (ClassNotFoundException e) {
			return Optional.empty();
		}
	}
}
