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

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * {@link PropertyValueConversions} provides access to {@link PropertyValueConverter converters} that may only be
 * applied to a specific property. Other than {@link org.springframework.core.convert.converter.Converter converters}
 * registered in {@link CustomConversions} the property based variants accept and allow returning {@literal null} values
 * and provide access to a store specific {@link PropertyValueConverter.ValueConversionContext conversion context}.
 * 
 * @author Christoph Strobl
 * @since ?
 * @currentBook The Desert Prince - Peter V. Brett
 */
public interface PropertyValueConversions {

	/**
	 * Check if a {@link PropertyValueConverter} is present for the given {@literal property}.
	 *
	 * @param property must not be {@literal null}.
	 * @return {@literal true} if a specific {@link PropertyValueConverter} is available.
	 */
	default boolean hasValueConverter(PersistentProperty<?> property) {
		return getValueConverter(property) != null;
	}

	/**
	 * Get the {@link PropertyValueConverter} for the given {@literal property} if present.
	 *
	 * @param property must not be {@literal null}. param <A> domain specific type
	 * @param <B> store native type
	 * @param <C> conversion context type
	 * @return the suitable {@link PropertyValueConverter} or {@literal null} if none available.
	 */
	@Nullable
	<A, B, C extends PropertyValueConverter.ValueConversionContext> PropertyValueConverter<A, B, C> getValueConverter(
			PersistentProperty<?> property);
}
