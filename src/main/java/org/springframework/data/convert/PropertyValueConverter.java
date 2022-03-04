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
 * @param <A> domain specific type.
 * @param <B> store native type.
 * @param <C> the store specific {@link ValueConversionContext conversion context}.
 * @since 2.7
 */
public interface PropertyValueConverter<A, B, C extends ValueConversionContext<? extends PersistentProperty<?>>> {

	/**
	 * Convert the given store specific value into it's domain value representation. Typically, a {@literal read}
	 * operation.
	 *
	 * @param value can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	A read(@Nullable B value, C context);

	/**
	 * Convert the given domain specific value into it's native store representation. Typically, a {@literal write}
	 * operation.
	 *
	 * @param value can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	B write(@Nullable A value, C context);

	/**
	 * NoOp {@link PropertyValueConverter} implementation.
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
	class FunctionPropertyValueConverter<A, B, P extends PersistentProperty<P>>
			implements PropertyValueConverter<A, B, ValueConversionContext<P>> {

		private final BiFunction<A, ValueConversionContext<P>, B> writer;
		private final BiFunction<B, ValueConversionContext<P>, A> reader;

		public FunctionPropertyValueConverter(BiFunction<A, ValueConversionContext<P>, B> writer,
				BiFunction<B, ValueConversionContext<P>, A> reader) {

			this.writer = writer;
			this.reader = reader;
		}

		@Nullable
		@Override
		public B write(@Nullable A value, ValueConversionContext<P> context) {
			return writer.apply(value, context);
		}

		@Nullable
		@Override
		public A read(@Nullable B value, ValueConversionContext<P> context) {
			return reader.apply(value, context);
		}
	}
}
