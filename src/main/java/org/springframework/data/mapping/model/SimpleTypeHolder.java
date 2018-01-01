/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.springframework.util.Assert;

/**
 * Simple container to hold a set of types to be considered simple types.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class SimpleTypeHolder {

	private static final Set<Class<?>> DEFAULTS = new HashSet<Class<?>>() {

		private static final long serialVersionUID = -1738594126505221500L;

		{
			add(boolean.class);
			add(boolean[].class);
			add(long.class);
			add(long[].class);
			add(short.class);
			add(short[].class);
			add(int.class);
			add(int[].class);
			add(byte.class);
			add(byte[].class);
			add(float.class);
			add(float[].class);
			add(double.class);
			add(double[].class);
			add(char.class);
			add(char[].class);
			add(Boolean.class);
			add(Long.class);
			add(Short.class);
			add(Integer.class);
			add(Byte.class);
			add(Float.class);
			add(Double.class);
			add(Character.class);
			add(String.class);
			add(Date.class);
			add(Locale.class);
			add(Class.class);
			add(Enum.class);
		}
	};
	public static final SimpleTypeHolder DEFAULT = new SimpleTypeHolder();

	private volatile Map<Class<?>, Boolean> simpleTypes;

	/**
	 * Creates a new {@link SimpleTypeHolder} containing the default types.
	 *
	 * @see #SimpleTypeHolder(Set, boolean)
	 */
	protected SimpleTypeHolder() {
		this(Collections.emptySet(), true);
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

		this.simpleTypes = new WeakHashMap<>(customSimpleTypes.size() + DEFAULTS.size());

		register(customSimpleTypes);

		if (registerDefaults) {
			register(DEFAULTS);
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

		this.simpleTypes = new WeakHashMap<>(customSimpleTypes.size() + source.simpleTypes.size());

		register(customSimpleTypes);
		registerCachePositives(source.simpleTypes);
	}

	private void registerCachePositives(Map<Class<?>, Boolean> source) {

		for (Entry<Class<?>, Boolean> entry : source.entrySet()) {

			if (!entry.getValue()) {
				continue;
			}

			this.simpleTypes.put(entry.getKey(), true);
		}
	}

	/**
	 * Returns whether the given type is considered a simple one.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public boolean isSimpleType(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		Map<Class<?>, Boolean> localSimpleTypes = this.simpleTypes;
		Boolean isSimpleType = localSimpleTypes.get(type);

		if (Object.class.equals(type)) {
			return true;
		}

		if (isSimpleType != null) {
			return isSimpleType;
		}

		if (type.getName().startsWith("java.lang")) {
			return true;
		}

		for (Class<?> simpleType : localSimpleTypes.keySet()) {

			if (simpleType.isAssignableFrom(type)) {

				isSimpleType = localSimpleTypes.get(simpleType);
				this.simpleTypes = put(localSimpleTypes, type, isSimpleType);
				return isSimpleType;
			}
		}

		this.simpleTypes = put(localSimpleTypes, type, false);
		return false;
	}

	private void register(Collection<? extends Class<?>> types) {
		types.forEach(customSimpleType -> this.simpleTypes.put(customSimpleType, true));
	}

	private static Map<Class<?>, Boolean> put(Map<Class<?>, Boolean> simpleTypes, Class<?> type, boolean isSimpleType) {

		Map<Class<?>, Boolean> copy = new WeakHashMap<>(simpleTypes);
		copy.put(type, isSimpleType);

		return copy;
	}
}
