/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.util;

/**
 * Wrapper to safely store {@literal null} values in the value cache.
 * 
 * @author Patryk Wasik
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class CacheValue<T> {

	private static final CacheValue<?> ABSENT = new CacheValue<Object>(null);

	private final T value;

	/**
	 * Creates a new {@link CacheValue} for the gven value.
	 * 
	 * @param type can be {@literal null}.
	 */
	private CacheValue(T type) {
		this.value = type;
	}

	/**
	 * Returns the actual underlying value.
	 * 
	 * @return
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Returns whether the cached value has an actual value.
	 * 
	 * @return
	 */
	public boolean isPresent() {
		return value != null;
	}

	/**
	 * Returns whether the cached value has the given actual value.
	 * 
	 * @param value can be {@literal null};
	 * @return
	 */
	public boolean hasValue(T value) {
		return isPresent() ? this.value.equals(value) : value == null;
	}

	/**
	 * Returns a new {@link CacheValue} for the given value.
	 * 
	 * @param value can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> CacheValue<T> ofNullable(T value) {
		return value == null ? (CacheValue<T>) ABSENT : new CacheValue<T>(value);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return isPresent() ? 0 : value.hashCode();
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof CacheValue)) {
			return false;
		}

		CacheValue<?> that = (CacheValue<?>) obj;

		return this.value == null ? false : this.value.equals(that.value);
	}
}
