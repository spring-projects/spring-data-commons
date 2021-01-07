/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Abstraction of a path of {@link PersistentProperty}s.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class DefaultPersistentPropertyPath<P extends PersistentProperty<P>> implements PersistentPropertyPath<P> {

	private static final Converter<PersistentProperty<?>, String> DEFAULT_CONVERTER = (source) -> source.getName();
	private static final String DEFAULT_DELIMITER = ".";

	private final List<P> properties;

	/**
	 * Creates a new {@link DefaultPersistentPropertyPath} for the given {@link PersistentProperty}s.
	 *
	 * @param properties must not be {@literal null}.
	 */
	public DefaultPersistentPropertyPath(List<P> properties) {

		Assert.notNull(properties, "Properties must not be null!");

		this.properties = properties;
	}

	/**
	 * Creates an empty {@link DefaultPersistentPropertyPath}.
	 *
	 * @return
	 */
	public static <T extends PersistentProperty<T>> DefaultPersistentPropertyPath<T> empty() {
		return new DefaultPersistentPropertyPath<T>(Collections.emptyList());
	}

	/**
	 * Appends the given {@link PersistentProperty} to the current {@link PersistentPropertyPath}.
	 *
	 * @param property must not be {@literal null}.
	 * @return a new {@link DefaultPersistentPropertyPath} with the given property appended to the current one.
	 * @throws IllegalArgumentException in case the property is not a property of the type of the current leaf property.
	 */
	public DefaultPersistentPropertyPath<P> append(P property) {

		Assert.notNull(property, "Property must not be null!");

		if (isEmpty()) {
			return new DefaultPersistentPropertyPath<>(Collections.singletonList(property));
		}

		@SuppressWarnings("null")
		Class<?> leafPropertyType = getLeafProperty().getActualType();

		Assert.isTrue(property.getOwner().getType().equals(leafPropertyType),
				() -> String.format("Cannot append property %s to type %s!", property.getName(), leafPropertyType.getName()));

		List<P> properties = new ArrayList<>(this.properties);
		properties.add(property);

		return new DefaultPersistentPropertyPath<>(properties);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath()
	 */
	@Nullable
	public String toDotPath() {
		return toPath(DEFAULT_DELIMITER, DEFAULT_CONVERTER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath(org.springframework.core.convert.converter.Converter)
	 */
	@Nullable
	public String toDotPath(Converter<? super P, String> converter) {
		return toPath(DEFAULT_DELIMITER, converter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toPath(java.lang.String)
	 */
	@Nullable
	public String toPath(String delimiter) {
		return toPath(delimiter, DEFAULT_CONVERTER);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toPath(java.lang.String, org.springframework.core.convert.converter.Converter)
	 */
	@Nullable
	public String toPath(String delimiter, Converter<? super P, String> converter) {

		Assert.hasText(delimiter, "Delimiter must not be null or empty!");
		Assert.notNull(converter, "Converter must not be null!");

		String result = properties.stream() //
				.map(converter::convert) //
				.filter(StringUtils::hasText) //
				.collect(Collectors.joining(delimiter));

		return result.isEmpty() ? null : result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getLeafProperty()
	 */
	@Nullable
	public P getLeafProperty() {
		return properties.isEmpty() ? null : properties.get(properties.size() - 1);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getBaseProperty()
	 */
	@Nullable
	public P getBaseProperty() {
		return properties.isEmpty() ? null : properties.get(0);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#isBasePathOf(org.springframework.data.mapping.context.PersistentPropertyPath)
	 */
	public boolean isBasePathOf(PersistentPropertyPath<P> path) {

		Assert.notNull(path, "PersistentPropertyPath must not be null!");

		Iterator<P> iterator = path.iterator();

		for (P property : this) {

			if (!iterator.hasNext()) {
				return false;
			}

			P reference = iterator.next();

			if (!property.equals(reference)) {
				return false;
			}
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getExtensionForBaseOf(org.springframework.data.mapping.context.PersistentPropertyPath)
	 */
	public PersistentPropertyPath<P> getExtensionForBaseOf(PersistentPropertyPath<P> base) {

		if (!base.isBasePathOf(this)) {
			return this;
		}

		List<P> result = new ArrayList<>();
		Iterator<P> iterator = iterator();

		for (int i = 0; i < base.getLength(); i++) {
			iterator.next();
		}

		while (iterator.hasNext()) {
			result.add(iterator.next());
		}

		return new DefaultPersistentPropertyPath<>(result);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getParentPath()
	 */
	public PersistentPropertyPath<P> getParentPath() {

		int size = properties.size();

		return size == 0 ? this : new DefaultPersistentPropertyPath<>(properties.subList(0, size - 1));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getLength()
	 */
	public int getLength() {
		return properties.size();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<P> iterator() {
		return properties.iterator();
	}

	/**
	 * Returns whether the current path contains a property of the given type.
	 *
	 * @param type can be {@literal null}.
	 * @return
	 */
	public boolean containsPropertyOfType(@Nullable TypeInformation<?> type) {

		return type == null //
				? false //
				: properties.stream() //
						.anyMatch(property -> type.equals(property.getTypeInformation().getActualType()));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof DefaultPersistentPropertyPath)) {
			return false;
		}

		DefaultPersistentPropertyPath<?> that = (DefaultPersistentPropertyPath<?>) o;
		return ObjectUtils.nullSafeEquals(properties, that.properties);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(properties);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	@Nullable
	public String toString() {
		return toDotPath();
	}
}
