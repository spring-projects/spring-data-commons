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
 * Simple lazy implementation using lazy value resolution. Lazy evaluation not guarded with locks and can therefore lead
 * to multiple invocations of the underlying {@link Supplier}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Henning Rohlfs
 * @author Johannes Englmeier
 * @author Greg Turnquist
 * @since 4.0
 */
class LazyDelegate<T extends @Nullable Object> {

	private final Supplier<T> supplier;

	private @Nullable T value;
	private volatile boolean resolved;

	LazyDelegate(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	/**
	 * Returns the value of the lazy evaluation.
	 *
	 * @return the value of the lazy evaluation, can be {@literal null}.
	 */
	@Nullable
	public T getNullable() {

		if (resolved) {
			return value;
		}

		T result = supplier.get();
		this.value = result;
		this.resolved = true;

		return result;
	}

	public boolean isResolved() {
		return resolved;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof LazyDelegate<?> lazy)) {
			return false;
		}

		if (resolved != lazy.resolved) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(supplier, lazy.supplier)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(value, lazy.value);
	}

	@Override
	public int hashCode() {

		int result = ObjectUtils.nullSafeHashCode(supplier);

		result = 31 * result + ObjectUtils.nullSafeHashCode(value);
		result = 31 * result + (resolved ? 1 : 0);

		return result;
	}

}
