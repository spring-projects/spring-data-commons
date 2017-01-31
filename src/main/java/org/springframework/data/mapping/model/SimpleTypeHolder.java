/*
 * Copyright 2011-2015 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.util.Assert;

/**
 * Simple container to hold a set of types to be considered simple types.
 * 
 * @author Oliver Gierke
 */
public class SimpleTypeHolder {

	private static final Set<Class<?>> DEFAULTS = new HashSet<Class<?>>();

	static {
		DEFAULTS.add(boolean.class);
		DEFAULTS.add(boolean[].class);
		DEFAULTS.add(long.class);
		DEFAULTS.add(long[].class);
		DEFAULTS.add(short.class);
		DEFAULTS.add(short[].class);
		DEFAULTS.add(int.class);
		DEFAULTS.add(int[].class);
		DEFAULTS.add(byte.class);
		DEFAULTS.add(byte[].class);
		DEFAULTS.add(float.class);
		DEFAULTS.add(float[].class);
		DEFAULTS.add(double.class);
		DEFAULTS.add(double[].class);
		DEFAULTS.add(char.class);
		DEFAULTS.add(char[].class);
		DEFAULTS.add(Boolean.class);
		DEFAULTS.add(Long.class);
		DEFAULTS.add(Short.class);
		DEFAULTS.add(Integer.class);
		DEFAULTS.add(Byte.class);
		DEFAULTS.add(Float.class);
		DEFAULTS.add(Double.class);
		DEFAULTS.add(Character.class);
		DEFAULTS.add(String.class);
		DEFAULTS.add(Date.class);
		DEFAULTS.add(Locale.class);
		DEFAULTS.add(Class.class);
		DEFAULTS.add(Enum.class);
	}

	private final Set<Class<?>> simpleTypes;

	/**
	 * Creates a new {@link SimpleTypeHolder} containing the default types.
	 * 
	 * @see #SimpleTypeHolder(Set, boolean)
	 */
	@SuppressWarnings("unchecked")
	public SimpleTypeHolder() {
		this(Collections.EMPTY_SET, true);
	}

	/**
	 * Creates a new {@link SimpleTypeHolder} to carry the given custom simple types. Registration of default simple types
	 * can be deactivated by passing {@literal false} for {@code registerDefaults}.
	 * 
	 * @param customSimpleTypes
	 * @param registerDefaults
	 */
	public SimpleTypeHolder(Set<? extends Class<?>> customSimpleTypes, boolean registerDefaults) {

		Assert.notNull(customSimpleTypes, "CustomSimpleTypes must not be null!");
		this.simpleTypes = new CopyOnWriteArraySet<Class<?>>(customSimpleTypes);

		if (registerDefaults) {
			this.simpleTypes.addAll(DEFAULTS);
		}
	}

	/**
	 * Copy constructor to create a new {@link SimpleTypeHolder} that carries the given additional custom simple types.
	 * 
	 * @param customSimpleTypes must not be {@literal null}
	 * @param source must not be {@literal null}
	 */
	public SimpleTypeHolder(Set<? extends Class<?>> customSimpleTypes, SimpleTypeHolder source) {

		Assert.notNull(customSimpleTypes, "CustomSimpleTypes must not be null!");
		Assert.notNull(source, "SourceTypeHolder must not be null!");

		this.simpleTypes = new CopyOnWriteArraySet<Class<?>>(customSimpleTypes);
		this.simpleTypes.addAll(source.simpleTypes);
	}

	/**
	 * Returns whether the given type is considered a simple one.
	 * 
	 * @param type
	 * @return
	 */
	public boolean isSimpleType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		if (Object.class.equals(type) || simpleTypes.contains(type)) {
			return true;
		}

		for (Class<?> clazz : simpleTypes) {
			if (clazz.isAssignableFrom(type)) {
				simpleTypes.add(type);
				return true;
			}
		}

		return false;
	}
}
