/*
 * Copyright 2021-present the original author or authors.
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

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.PersistentProperty;

/**
 * {@link PropertyValueConverter} provides a symmetric way of converting certain properties from domain to
 * store-specific values.
 * <p>
 * A {@link PropertyValueConverter} is, other than a {@link ReadingConverter} or {@link WritingConverter}, only applied
 * to special annotated fields which allows a fine-grained conversion of certain values within a specific context.
 * <p>
 * Converter methods are called with non-null values only and provide specific hooks for {@code null} value handling.
 * {@link #readNull(ValueConversionContext)} and {@link #writeNull(ValueConversionContext)} methods are specifically
 * designated to either retain {@code null} values or return a different value to indicate {@code null} values.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @param <DV> domain-specific type.
 * @param <SV> store-native type.
 * @param <C> the store specific {@link ValueConversionContext conversion context}.
 * @see org.springframework.data.convert.ValueConversionContext
 * @see org.springframework.data.mapping.PersistentProperty
 * @since 2.7
 */
public interface PropertyValueConverter<DV, SV, C extends ValueConversionContext<? extends PersistentProperty<?>>> {

	/**
	 * Convert the given store specific value into it's domain value representation. Typically, a {@literal read}
	 * operation.
	 *
	 * @param value value to read.
	 * @param context {@link ValueConversionContext} containing store-specific metadata used in the value conversion;
	 *          never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	DV read(SV value, C context);

	/**
	 * Convert the given {@code null} value from the store into its domain value representation. Typically, a
	 * {@literal read} operation. Returns {@code null} by default.
	 *
	 * @param context {@link ValueConversionContext} containing store-specific metadata used in the value conversion;
	 *          never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	default DV readNull(C context) {
		return null;
	}

	/**
	 * Convert the given domain-specific value into it's native store representation. Typically, a {@literal write}
	 * operation.
	 *
	 * @param value value to write; can be {@literal null}.
	 * @param context {@link ValueConversionContext} containing store-specific metadata used in the value conversion;
	 *          never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	SV write(DV value, C context);

	/**
	 * Convert the given {@code null} value from the domain model into it's native store representation. Typically, a
	 * {@literal write} operation. Returns {@code null} by default.
	 *
	 * @param context {@link ValueConversionContext} containing store-specific metadata used in the value conversion;
	 *          never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	default @Nullable SV writeNull(C context) {
		return null;
	}

	/**
	 * No-op {@link PropertyValueConverter} implementation.
	 *
	 * @author Christoph Strobl
	 */
	@SuppressWarnings({ "rawtypes", "null" })
	enum ObjectToObjectPropertyValueConverter implements PropertyValueConverter {

		INSTANCE;

		@Override
		public @Nullable Object read(@Nullable Object value, ValueConversionContext context) {
			return value;
		}

		@Override
		public @Nullable Object write(@Nullable Object value, ValueConversionContext context) {
			return value;
		}
	}

	/**
	 * A {@link PropertyValueConverter} that delegates conversion to the given {@link BiFunction}s.
	 *
	 * @author Oliver Drotbohm
	 */
	class FunctionPropertyValueConverter<DV, SV, P extends PersistentProperty<P>>
			implements PropertyValueConverter<DV, SV, ValueConversionContext<P>> {

		private final BiFunction<DV, ValueConversionContext<P>, SV> writer;
		private final BiFunction<SV, ValueConversionContext<P>, DV> reader;

		public FunctionPropertyValueConverter(BiFunction<DV, ValueConversionContext<P>, SV> writer,
				BiFunction<SV, ValueConversionContext<P>, DV> reader) {

			this.writer = writer;
			this.reader = reader;
		}

		@Override
		public @Nullable SV write(@Nullable DV value, ValueConversionContext<P> context) {
			return writer.apply(value, context);
		}

		@Override
		public @Nullable SV writeNull(ValueConversionContext<P> context) {
			return writer.apply(null, context);
		}

		@Override
		public @Nullable DV read(@Nullable SV value, ValueConversionContext<P> context) {
			return reader.apply(value, context);
		}

		@Override
		public @Nullable DV readNull(ValueConversionContext<P> context) {
			return reader.apply(null, context);
		}
	}
}
