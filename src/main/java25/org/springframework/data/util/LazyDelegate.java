/*
 * Copyright 2016-2025 the original author or authors.
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
package org.springframework.data.util;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Lazy adapter using {@link StableValue}.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class LazyDelegate<T extends @Nullable Object> {

	private final Supplier<T> supplier;
	private final StableValue<T> value;

	LazyDelegate(Supplier<T> supplier) {

		System.out.println("Stable");
		this.supplier = supplier;
		this.value = StableValue.of();
	}

	/**
	 * Returns the value of the lazy evaluation.
	 *
	 * @return the value of the lazy evaluation, can be {@literal null}.
	 */
	@Nullable
	public T getNullable() {
		return value.orElseSet(supplier);
	}

	public boolean isResolved() {
		return value.isSet();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof LazyDelegate<?> lazy)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(value, lazy.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

}
