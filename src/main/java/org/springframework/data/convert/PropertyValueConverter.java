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

import org.springframework.data.convert.PropertyValueConverter.ValueConversionContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * {@link PropertyValueConverter} provides a symmetric way of converting certain properties from domain to store
 * specific values.
 * <p>
 * A {@link PropertyValueConverter} is, other than a {@link ReadingConverter} or {@link WritingConverter}, only applied
 * to special annotated fields which allows a fine grained conversion of certain values within a specific context.
 *
 * @author Christoph Strobl
 * @param <A> domain specific type
 * @param <B> store native type
 * @since 2.7
 */
public interface PropertyValueConverter<A, B, C extends ValueConversionContext<? extends PersistentProperty<?>>> {

	/**
	 * Convert the given store specific value into it's domain value representation. Typically a {@literal read}
	 * operation.
	 *
	 * @param nativeValue can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	A /*read*/ nativeToDomain(@Nullable B nativeValue, C context);

	/**
	 * Convert the given domain specific value into it's native store representation. Typically a {@literal write}
	 * operation.
	 *
	 * @param domainValue can be {@literal null}.
	 * @param context never {@literal null}.
	 * @return the converted value. Can be {@literal null}.
	 */
	@Nullable
	B /*write*/ domainToNative(@Nullable A domainValue, C context);

	/**
	 * @author Christoph Strobl
	 * @author Oliver Drotbohm
	 */
	interface ValueConversionContext<P extends PersistentProperty<P>> {

		/**
		 * Return the {@link PersistentProperty} to be handled.
		 *
		 * @return will never be {@literal null}.
		 */
		P getProperty();

		/**
		 * Write to whatever type is considered best for the given source.
		 *
		 * @param value
		 * @return
		 */
		@Nullable
		default Object write(@Nullable Object value) {
			return null;
		}

		/**
		 * Write as the given type.
		 *
		 * @param value can be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		default <T> T write(@Nullable Object value, Class<T> target) {
			return write(value, ClassTypeInformation.from(target));
		}

		/**
		 * Write as the given type.
		 *
		 * @param value can be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		default <T> T write(@Nullable Object value, TypeInformation<T> target) {
			return null;
		}

		/**
		 * Reads the value into the type of the current property.
		 *
		 * @param value can be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		default Object read(@Nullable Object value) {
			return read(value, getProperty().getTypeInformation());
		}

		/**
		 * Reads the value as the given type.
		 *
		 * @param value can be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		default <T> T read(@Nullable Object value, Class<T> target) {
			return null;
		}

		/**
		 * Reads the value as the given type.
		 *
		 * @param value can be {@literal null}.
		 * @param target must not be {@literal null}.
		 * @return can be {@literal null}.
		 */
		@Nullable
		default <T> T read(@Nullable Object value, TypeInformation<T> target) {
			return null;
		}
	}

	/**
	 * NoOp {@link PropertyValueConverter} implementation.
	 *
	 * @author Christoph Strobl
	 */
	@SuppressWarnings({ "rawtypes", "null" })
	enum ObjectToObjectPropertyValueConverter implements PropertyValueConverter {

		INSTANCE;

		@Override
		public Object nativeToDomain(Object value, ValueConversionContext context) {
			return value;
		}

		@Override
		public Object domainToNative(Object value, ValueConversionContext context) {
			return value;
		}
	}
}
