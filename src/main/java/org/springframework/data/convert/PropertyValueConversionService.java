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
import org.springframework.util.Assert;

/**
 * Conversion service based on {@link CustomConversions} to convert domain and store values using
 * {@link PropertyValueConverter property-specific converters}.
 *
 * @author Mark Paluch
 * @since 2.7
 */
public class PropertyValueConversionService {

	private final CustomConversions conversions;

	public PropertyValueConversionService(CustomConversions conversions) {

		Assert.notNull(conversions, "CustomConversions must not be null");

		this.conversions = conversions;
	}

	/**
	 * Return {@literal true} there is a converter registered for {@link PersistentProperty}.
	 * <p>
	 * If this method returns {@literal true}, it means {@link #read(Object, PersistentProperty, ValueConversionContext)}
	 * and {@link #write(Object, PersistentProperty, ValueConversionContext)} are capable to invoke conversion.
	 *
	 * @param property the underlying property.
	 * @return {@literal true} there is a converter registered for {@link PersistentProperty}.
	 */
	public boolean hasConverter(PersistentProperty<?> property) {
		return conversions.hasPropertyValueConverter(property);
	}

	/**
	 * Convert a value from its store-native representation into its domain-specific type.
	 *
	 * @param value the value to convert. Can be {@code null}.
	 * @param property the underlying property.
	 * @param context the context object.
	 * @param <P> property type.
	 * @param <VCC> value conversion context type.
	 * @return the value to be used in the domain model. Can be {@code null}.
	 */
	@Nullable
	public <P extends PersistentProperty<P>, VCC extends ValueConversionContext<P>> Object read(@Nullable Object value,
			P property, VCC context) {

		PropertyValueConverter<Object, Object, ValueConversionContext<P>> converter = getRequiredConverter(property);

		if (value == null) {
			return converter.readNull(context);
		}

		return converter.read(value, context);
	}

	/**
	 * Convert a value from its domain-specific value into its store-native representation.
	 *
	 * @param value the value to convert. Can be {@code null}.
	 * @param property the underlying property.
	 * @param context the context object.
	 * @param <P> property type.
	 * @param <VCC> value conversion context type.
	 * @return the value to be written to the data store. Can be {@code null}.
	 */
	@Nullable
	public <P extends PersistentProperty<P>, VCC extends ValueConversionContext<P>> Object write(@Nullable Object value,
			P property, VCC context) {

		PropertyValueConverter<Object, Object, ValueConversionContext<P>> converter = getRequiredConverter(property);

		if (value == null) {
			return converter.writeNull(context);
		}

		return converter.write(value, context);
	}

	private <P extends PersistentProperty<P>> PropertyValueConverter<Object, Object, ValueConversionContext<P>> getRequiredConverter(
			P property) {

		PropertyValueConverter<Object, Object, ValueConversionContext<P>> converter = conversions
				.getPropertyValueConverter(property);

		if (converter == null) {
			throw new IllegalArgumentException(String.format("No converter registered for property %s", property));
		}

		return converter;
	}
}
