/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.Nullable;

/**
 * {@link PropertyValueConverter} provides a symmetric way of converting certain properties from domain to
 * store-specific values.
 * <p>
 * A {@link PropertyValueConverter} is, other than a {@link ReadingConverter} or {@link WritingConverter}, only applied
 * to special annotated fields which allows a fine-grained conversion of certain values within a specific context.
 *
 * @author Christoph Strobl
 * @param <DV> domain-specific type.
 * @param <SV> store-native type.
 * @param <C> the store specific {@link ValueConversionContext conversion context}.
 * @since 2.7
 */
public interface PropertyValueConverter<DV, SV, C extends ValueConversionContext<? extends PersistentProperty<?>>> {

	/**
	 * Convert the given store specific value into it's domain value representation. Typically, a {@literal read}
	 * operation.
	 *
	 * @param value can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	DV read(@Nullable SV value, C context);

	/**
	 * Convert the given domain-specific value into it's native store representation. Typically, a {@literal write}
	 * operation.
	 *
	 * @param value can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	SV write(@Nullable DV value, C context);

	/**
	 * No-op {@link PropertyValueConverter} implementation.
	 *
	 * @author Christoph Strobl
	 */
	@SuppressWarnings({ "rawtypes", "null" })
	enum ObjectToObjectPropertyValueConverter implements PropertyValueConverter {

		INSTANCE;

		@Nullable
		@Override
		public Object read(@Nullable Object value, ValueConversionContext context) {
			return value;
		}

		@Nullable
		@Override
		public Object write(@Nullable Object value, ValueConversionContext context) {
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

		@Nullable
		@Override
		public SV write(@Nullable DV value, ValueConversionContext<P> context) {
			return writer.apply(value, context);
		}

		@Nullable
		@Override
		public DV read(@Nullable SV value, ValueConversionContext<P> context) {
			return reader.apply(value, context);
		}
	}
}
