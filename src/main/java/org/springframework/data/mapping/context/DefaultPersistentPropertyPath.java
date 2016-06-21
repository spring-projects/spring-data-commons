/*
 * Copyright 2011-2015 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstraction of a path of {@link PersistentProperty}s.
 * 
 * @author Oliver Gierke
 */
class DefaultPersistentPropertyPath<T extends PersistentProperty<T>> implements PersistentPropertyPath<T> {

	private enum PropertyNameConverter implements Converter<PersistentProperty<?>, String> {

		INSTANCE;

		public String convert(PersistentProperty<?> source) {
			return source.getName();
		}
	}

	private final List<T> properties;

	/**
	 * Creates a new {@link DefaultPersistentPropertyPath} for the given {@link PersistentProperty}s.
	 * 
	 * @param properties must not be {@literal null}.
	 */
	public DefaultPersistentPropertyPath(List<T> properties) {

		Assert.notNull(properties);

		this.properties = properties;
	}

	/**
	 * Creates an empty {@link DefaultPersistentPropertyPath}.
	 * 
	 * @return
	 */
	public static <T extends PersistentProperty<T>> DefaultPersistentPropertyPath<T> empty() {
		return new DefaultPersistentPropertyPath<>(Collections.<T> emptyList());
	}

	/**
	 * Appends the given {@link PersistentProperty} to the current {@link PersistentPropertyPath}.
	 * 
	 * @param property must not be {@literal null}.
	 * @return a new {@link DefaultPersistentPropertyPath} with the given property appended to the current one.
	 * @throws IllegalArgumentException in case the property is not a property of the type of the current leaf property.
	 */
	public DefaultPersistentPropertyPath<T> append(T property) {

		Assert.notNull(property, "Property must not be null!");

		if (isEmpty()) {
			return new DefaultPersistentPropertyPath<>(Arrays.asList(property));
		}

		Class<?> leafPropertyType = getLeafProperty().getActualType();
		Assert.isTrue(property.getOwner().getType().equals(leafPropertyType),
				String.format("Cannot append property %s to type %s!", property.getName(), leafPropertyType.getName()));

		List<T> properties = new ArrayList<>(this.properties);
		properties.add(property);

		return new DefaultPersistentPropertyPath<>(properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath()
	 */
	public String toDotPath() {
		return toPath(null, null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toDotPath(org.springframework.core.convert.converter.Converter)
	 */
	public String toDotPath(Converter<? super T, String> converter) {
		return toPath(null, converter);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toPath(java.lang.String)
	 */
	public String toPath(String delimiter) {
		return toPath(delimiter, null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#toPath(java.lang.String, org.springframework.core.convert.converter.Converter)
	 */
	public String toPath(String delimiter, Converter<? super T, String> converter) {

		@SuppressWarnings("unchecked")
		Converter<? super T, String> converterToUse = (Converter<? super T, String>) (converter == null
				? PropertyNameConverter.INSTANCE : converter);
		String delimiterToUse = delimiter == null ? "." : delimiter;

		List<String> result = new ArrayList<>();

		for (T property : properties) {

			String convert = converterToUse.convert(property);

			if (StringUtils.hasText(convert)) {
				result.add(convert);
			}
		}

		return result.isEmpty() ? null : StringUtils.collectionToDelimitedString(result, delimiterToUse);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getLeafProperty()
	 */
	public T getLeafProperty() {
		return properties.get(properties.size() - 1);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#getBaseProperty()
	 */
	public T getBaseProperty() {
		return properties.get(0);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#isBasePathOf(org.springframework.data.mapping.context.PersistentPropertyPath)
	 */
	public boolean isBasePathOf(PersistentPropertyPath<T> path) {

		if (path == null) {
			return false;
		}

		Iterator<T> iterator = path.iterator();

		for (T property : this) {

			if (!iterator.hasNext()) {
				return false;
			}

			T reference = iterator.next();

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
	public PersistentPropertyPath<T> getExtensionForBaseOf(PersistentPropertyPath<T> base) {

		if (!base.isBasePathOf(this)) {
			return this;
		}

		List<T> result = new ArrayList<>();
		Iterator<T> iterator = iterator();

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
	public PersistentPropertyPath<T> getParentPath() {
		int size = properties.size();
		if (size <= 1) {
			return this;
		}
		return new DefaultPersistentPropertyPath<>(properties.subList(0, size - 1));
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
	public Iterator<T> iterator() {
		return properties.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.PersistentPropertyPath#isEmpty()
	 */
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}

		DefaultPersistentPropertyPath<?> that = (DefaultPersistentPropertyPath<?>) obj;

		return this.properties.equals(that.properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toDotPath();
	}
}
