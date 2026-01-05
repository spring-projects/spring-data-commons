/*
 * Copyright 2022-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.PersistentProperty;

/**
 * The {@link ValueConversionContext} provides access to the store-specific {@link PersistentProperty} and allows to
 * call the store-default conversion through the {@literal read}/{@literal write} methods.
 * <p>
 * Store implementations should provide their own flavor of {@link ValueConversionContext} enhancing the existing API,
 * implementing delegates for {@link #read(Object, TypeInformation)}, {@link #write(Object, TypeInformation)}.
 *
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 * @see org.springframework.data.mapping.PersistentProperty
 */
public interface ValueConversionContext<P extends PersistentProperty<P>> {

	/**
	 * Return the {@link PersistentProperty} to be handled.
	 *
	 * @return will never be {@literal null}.
	 * @see org.springframework.data.mapping.PersistentProperty
	 */
	P getProperty();

	/**
	 * Write the value as an instance of the {@link PersistentProperty#getTypeInformation() property type}.
	 *
	 * @param value {@link Object value} to write; can be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be written as an instance of the
	 *           {@link PersistentProperty#getTypeInformation() property type}.
	 * @see PersistentProperty#getTypeInformation()
	 * @see #write(Object, TypeInformation)
	 */
	default @Nullable Object write(@Nullable Object value) {
		return write(value, getProperty().getTypeInformation());
	}

	/**
	 * Write the value as an instance of {@link Class type}.
	 *
	 * @param value {@link Object value} to write; can be {@literal null}.
	 * @param target {@link Class type} of value to be written; must not be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be written as an instance of {@link Class type}.
	 * @see #write(Object, TypeInformation)
	 * @see TypeInformation
	 */
	default <T> @Nullable T write(@Nullable Object value, Class<T> target) {
		return write(value, TypeInformation.of(target));
	}

	/**
	 * Write the value as an instance of {@link TypeInformation type}.
	 *
	 * @param value {@link Object value} to write; can be {@literal null}.
	 * @param target {@link TypeInformation type} of value to be written; must not be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be written as an instance of {@link TypeInformation type}.
	 * @see TypeInformation
	 */
	default <T> @Nullable T write(@Nullable Object value, TypeInformation<T> target) {

		if (value == null || target.getType().isInstance(value)) {
			return target.getType().cast(value);
		}

		throw new IllegalStateException(String.format(
				"%s does not provide write function that allows value conversion to target type (%s)", getClass(), target));
	}

	/**
	 * Reads the value as an instance of the {@link PersistentProperty#getTypeInformation() property type}.
	 *
	 * @param value {@link Object value} to be read; can be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be read as an instance of the
	 *           {@link PersistentProperty#getTypeInformation() property type}.
	 * @see PersistentProperty#getTypeInformation()
	 * @see #read(Object, TypeInformation)
	 */
	default @Nullable Object read(@Nullable Object value) {
		return read(value, getProperty().getTypeInformation());
	}

	/**
	 * Reads the value as an instance of {@link Class type}.
	 *
	 * @param value {@link Object value} to be read; can be {@literal null}.
	 * @param target {@link Class type} of value to be read; must not be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be read as an instance of {@link Class type}.
	 * @see #read(Object, TypeInformation)
	 * @see TypeInformation
	 */
	default <T> @Nullable T read(@Nullable Object value, Class<T> target) {
		return read(value, TypeInformation.of(target));
	}

	/**
	 * Reads the value as an instance of {@link TypeInformation type}.
	 *
	 * @param value {@link Object value} to be read; can be {@literal null}.
	 * @param target {@link TypeInformation type} of value to be read; must not be {@literal null}.
	 * @return can be {@literal null}.
	 * @throws IllegalStateException if value cannot be read as an instance of {@link TypeInformation type}.
	 * @see TypeInformation
	 */
	default <T> @Nullable T read(@Nullable Object value, TypeInformation<T> target) {

		if (value == null || target.getType().isInstance(value)) {
			return target.getType().cast(value);
		}

		throw new IllegalStateException(String.format(
				"%s does not provide read function that allows value conversion to target type (%s)", getClass(), target));
	}
}
