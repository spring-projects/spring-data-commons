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
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * The {@link ValueConversionContext} provides access to the store specific {@link PersistentProperty} and allows to
 * call the store default conversion via the {@literal read}/{@literal write} methods.
 * <p>
 * Store implementations should provide their own flavor of {@link ValueConversionContext} enhancing the existing API,
 * implementing delegates for {@link #read(Object, TypeInformation)}, {@link #write(Object, TypeInformation)}.
 *
 * @author Christoph Strobl
 * @author Oliver Drotbohm
 */
public interface ValueConversionContext<P extends PersistentProperty<P>> {

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
		return write(value, getProperty().getTypeInformation());
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
		throw new IllegalStateException(String.format(
				"%s does not provide write function that allows value conversion to target type (%s).", getClass(), target));
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
		return read(value, ClassTypeInformation.from(target));
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
		throw new IllegalStateException(String.format(
				"%s does not provide write function that allows value conversion to target type (%s).", getClass(), target));
	}
}
