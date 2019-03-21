/*
 * Copyright 2014 the original author or authors.
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

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Wrapper around Spring's {@link org.springframework.core.CollectionFactory} to add support for additional, sepcial
 * collection types.
 * 
 * @author Oliver Gierke
 */
public abstract class CollectionFactory {

	private CollectionFactory() {}

	/**
	 * Creates a new collection instance for the given collection type. Might also inspect the element type in case
	 * special collections are requested (e.g. {@link EnumSet}).
	 * 
	 * @param collectionType must not be {@literal null}.
	 * @param elementType can be {@literal null}.
	 * @param size the initial size of the collection to be created.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> createCollection(Class<?> collectionType, Class<?> elementType, int size) {

		Assert.notNull(collectionType, "Collection type must not be null!");

		if (EnumSet.class.equals(collectionType)) {
			return EnumSet.noneOf(asEnumType(elementType));
		}

		return org.springframework.core.CollectionFactory.createCollection(collectionType, size);
	}

	/**
	 * Creates a new map instance for the given map type. Might also inspect the key type in case special maps are
	 * requested (e.g. {@link EnumMap}).
	 * 
	 * @param mapType must not be {@literal null}.
	 * @param keyType can be {@literal null}.
	 * @param size the initial size of the collection to be created.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map<Object, Object> createMap(Class<?> mapType, Class<?> keyType, int size) {

		Assert.notNull(mapType, "Map type must not be null!");

		if (EnumMap.class.isAssignableFrom(mapType)) {
			return new EnumMap(asEnumType(keyType));
		}

		return org.springframework.core.CollectionFactory.createMap(mapType, size);
	}

	/**
	 * Returns the given type as subtype of {@link Enum}.
	 * 
	 * @param enumType must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException in case the given type is not a subtype of {@link Enum}.
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {

		Assert.notNull(enumType, "EnumType must not be null!");

		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException(String.format("Given type %s is not an enum type!", enumType.getName()));
		}

		return enumType.asSubclass(Enum.class);
	}
}
