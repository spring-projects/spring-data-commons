/*
 * Copyright 2022 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 * @since ?
 */
public class PropertyValueConverterRegistrar {

	private final Map<Key, PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext>> converterRegistrationMap = new LinkedHashMap<>();

	boolean hasConverterFor(Class<?> type, String path) {
		return converterRegistrationMap.containsKey(new Key(type, path));
	}

	@Nullable
	public PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext> getConverter(Class<?> type, String path) {
		return converterRegistrationMap.get(new Key(type, path));
	}

	public int size() {
		return converterRegistrationMap.size();
	}

	public boolean isEmpty() {
		return converterRegistrationMap.isEmpty();
	}

	public PropertyValueConverterRegistrar register(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext> converter) {

		converterRegistrationMap.put(new Key(type, path), converter);
		return this;
	}

	public Collection<PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext>> converters() {
		return converterRegistrationMap.values();
	}

	public PropertyValueConverterRegistrar registerIfAbsent(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends PropertyValueConverter.ValueConversionContext> converter) {
		converterRegistrationMap.putIfAbsent(new Key(type, path), converter);
		return this;
	}

	static class Key {

		Class<?> type;
		String path;

		public Key(Class<?> type, String path) {
			this.type = type;
			this.path = path;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Key key = (Key) o;

			if (!ObjectUtils.nullSafeEquals(type, key.type)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(path, key.path);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(path);
			return result;
		}
	}
}
