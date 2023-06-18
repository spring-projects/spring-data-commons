/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.ObjectUtils;

/**
 * A registry of property specific {@link PropertyValueConverter value convertes} that may be used to convert only
 * specific properties/values of an object.
 *
 * @param <P> persistent property type.
 * @author Christoph Strobl
 * @since 2.7
 */
public class SimplePropertyValueConverterRegistry<P extends PersistentProperty<P>>
		implements ValueConverterRegistry<P> {

	private final Map<Key, PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>>> converterRegistrationMap = new LinkedHashMap<>();

	public SimplePropertyValueConverterRegistry() {}

	SimplePropertyValueConverterRegistry(SimplePropertyValueConverterRegistry<P> source) {
		this.converterRegistrationMap.putAll(source.converterRegistrationMap);
	}

	@Override
	public void registerConverter(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>> converter) {

		converterRegistrationMap.put(new Key(type, path), converter);
	}

	/**
	 * Register the {@link PropertyValueConverter} for the property of the given type if none had been registered before.
	 *
	 * @param type the target type.
	 * @param path the property name.
	 * @param converter the converter to register.
	 */
	public void registerConverterIfAbsent(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>> converter) {
		converterRegistrationMap.putIfAbsent(new Key(type, path), converter);
	}

	@Override
	public boolean containsConverterFor(Class<?> type, String path) {
		return converterRegistrationMap.containsKey(new Key(type, path));
	}

	@Override
	public <S, T> PropertyValueConverter<S, T, ? extends ValueConversionContext<P>> getConverter(Class<?> type,
			String path) {

		return (PropertyValueConverter<S, T, ? extends ValueConversionContext<P>>) converterRegistrationMap
				.get(new Key(type, path));
	}

	/**
	 * @return the number of registered converters.
	 */
	public int size() {
		return converterRegistrationMap.size();
	}

	public boolean isEmpty() {
		return converterRegistrationMap.isEmpty();
	}

	/**
	 * Obtain the underlying (mutable) map of converters.
	 *
	 * @return never {@literal null}.
	 */
	Map<Key, PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>>> getConverterRegistrationMap() {
		return converterRegistrationMap;
	}

	static class Key {

		final Class<?> type;
		final String path;

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
