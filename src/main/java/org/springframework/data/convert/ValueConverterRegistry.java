/*
 * Copyright 2022-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https//www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.convert;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * A registry of {@link PersistentProperty property-specific} {@link PropertyValueConverter value converters}
 * to convert only specific properties/values of an object.
 *
 * @author Christoph Strobl
 * @see PropertyValueConverter
 * @since 2.7
 */
public interface ValueConverterRegistry<P extends PersistentProperty<P>> {

	/**
	 * Register the {@link PropertyValueConverter} for the {@link PersistentProperty property} of the given type.
	 *
	 * @param type the target type. Must not be {@literal null}.
	 * @param path the property name. Must not be {@literal null}.
	 * @param converter the converter to register. Must not be {@literal null}.
	 */
	void registerConverter(Class<?> type, String path,
			PropertyValueConverter<?, ?, ? extends ValueConversionContext<P>> converter);

	/**
	 * Obtain the {@link PropertyValueConverter} registered for the given type, path combination or {@literal null}
	 * if none defined.
	 *
	 * @param type the target type. Must not be {@literal null}.
	 * @param path the property name. Must not be {@literal null}.
	 * @param <DV> domain-specific type.
	 * @param <SV> store-specific type.
	 * @return {@literal null} if no converter present for the given type/path combination.
	 */
	@Nullable
	<DV, SV> PropertyValueConverter<DV, SV, ? extends ValueConversionContext<P>> getConverter(Class<?> type, String path);

	/**
	 * Check if a converter is registered for the given type, path combination.
	 *
	 * @param type the target type. Must not be {@literal null}.
	 * @param path the property name. Must not be {@literal null}.
	 * @return {@literal false} if no converter is present for the given type/path combination.
	 */
	default boolean containsConverterFor(Class<?> type, String path) {
		return getConverter(type, path) != null;
	}

	/**
	 * Check if converters are registered.
	 */
	boolean isEmpty();

	/**
	 * Obtain a simple {@link ValueConverterRegistry}.
	 *
	 * @param <P> {@link PersistentProperty} type.
	 * @return new instance of {@link ValueConverterRegistry}.
	 */
	static <P extends PersistentProperty<P>> ValueConverterRegistry<P> simple() {
		return new SimplePropertyValueConverterRegistry<>();
	}
}
