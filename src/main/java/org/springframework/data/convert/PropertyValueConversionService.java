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

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Conversion service based on {@link CustomConversions} used to convert domain and store values using
 * {@link PropertyValueConverter property-specific converters}.
 *
 * @author Mark Paluch
 * @see PropertyValueConverter
 * @since 2.7
 */
public class PropertyValueConversionService {

	private final PropertyValueConversions conversions;

	/**
	 * Constructs a new instance of the {@link PropertyValueConversionService} initialized with the given,
	 * required {@link CustomConversions} for resolving the {@link PropertyValueConversions} used to
	 * convert {@link PersistentProperty} values during data access operations.
	 *
	 * @param conversions {@link CustomConversions} used to handle domain and store type conversions;
	 * must not be {@literal null}.
	 * @throws IllegalArgumentException if {@link CustomConversions} is {@literal null}.
	 * @see CustomConversions
	 */
	public PropertyValueConversionService(@NonNull CustomConversions conversions) {

		Assert.notNull(conversions, "CustomConversions must not be null");

		PropertyValueConversions valueConversions = conversions.getPropertyValueConversions();

		this.conversions = valueConversions != null ? valueConversions : NoOpPropertyValueConversions.INSTANCE;
	}

	/**
	 * Return {@literal true} if a {@link PropertyValueConverter} is registered for the {@link PersistentProperty}.
	 * <p>
	 * If this method returns {@literal true}, it means {@link #read(Object, PersistentProperty, ValueConversionContext)}
	 * and {@link #write(Object, PersistentProperty, ValueConversionContext)} are capable of handling conversions.
	 *
	 * @param property {@link PersistentProperty property} to evaluate for registration.
	 * @return {@literal true} if a {@link PropertyValueConverter} is registered for the {@link PersistentProperty}.
	 * @see PersistentProperty
	 */
	public boolean hasConverter(PersistentProperty<?> property) {
		return conversions.hasValueConverter(property);
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

		PropertyValueConverter<Object, Object, ValueConversionContext<P>> converter = conversions
				.getValueConverter(property);

		return value != null ? converter.read(value, context)
				: converter.readNull(context);
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

		PropertyValueConverter<Object, Object, ValueConversionContext<P>> converter = conversions
				.getValueConverter(property);

		return value != null ? converter.write(value, context)
				: converter.writeNull(context);
	}

	enum NoOpPropertyValueConversions implements PropertyValueConversions {

		INSTANCE;

		@Override
		public boolean hasValueConverter(PersistentProperty<?> property) {
			return false;
		}

		@Override
		public <DV, SV, P extends PersistentProperty<P>, VCC extends ValueConversionContext<P>> PropertyValueConverter<DV, SV, VCC> getValueConverter(
				P property) {
			throw new UnsupportedOperationException("No PropertyValueConversions was configured");
		}
	}
}
