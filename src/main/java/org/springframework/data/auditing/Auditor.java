/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.auditing;

import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Value Object encapsulating the actual auditor value.
 *
 * @author Christoph Strobl
 * @since 2.4
 */
class Auditor<T> {

	private static final Auditor NONE = new Auditor(null) {

		@Override
		public boolean isPresent() {
			return false;
		}
	};

	private final @Nullable T value;

	private Auditor(@Nullable T value) {
		this.value = value;
	}

	/**
	 * @return
	 */
	@Nullable
	public T getValue() {
		return value;
	}

	/**
	 * Create an {@link Auditor} for the given {@literal source} value. <br />
	 * If the given {@literal source} is {@literal null} {@link Auditor#none()} is returned. A source that already is an
	 * {@link Auditor} gets returned as is.
	 *
	 * @param source can be {@literal null}.
	 * @param <T>
	 * @return {@link Auditor#none()} if the given {@literal source} is {@literal null}. }
	 */
	public static <T> Auditor<T> of(@Nullable T source) {

		if (source instanceof Auditor) {
			return (Auditor) source;
		}

		return source == null ? Auditor.none() : new Auditor<>(source);
	}

	/**
	 * Create an {@link Auditor} for the given {@link Optional} value. <br />
	 * If the given {@literal source} is {@link Optional#empty()} {@link Auditor#none()} is returned. An {@link Optional}
	 * wrapping and {@link Auditor} returns the unwrapped {@link Auditor} instance as is.
	 *
	 * @param source must not be {@literal null}.
	 * @param <T>
	 * @return {@link Auditor#none()} if the given {@literal source} is {@literal null}. }
	 */
	public static <T> Auditor<T> ofOptional(@Nullable Optional<T> source) {
		return Auditor.of(source.orElse(null));
	}

	/**
	 * Return an {@link Auditor} that is not present.
	 *
	 * @param <T>
	 * @return never {@literal null}.
	 */
	public static <T> Auditor<T> none() {
		return NONE;
	}

	/**
	 * @return {@literal true} if {@link #getValue()} returns a non {@literal null} value.
	 */
	public boolean isPresent() {
		return getValue() != null;
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "Auditor.none()";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Auditor<?> auditor = (Auditor<?>) o;

		return ObjectUtils.nullSafeEquals(value, auditor.value);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(value);
	}
}
