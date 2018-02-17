/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Utility class for working with {@link Iterable} objects.
 *
 * @author John Blum
 * @see java.lang.Iterable
 * @since 2.1.0
 */
public abstract class IterableUtils {

	/**
	 * Determines whether the given {@link Iterable} is empty.
	 *
	 * @param iterable {@link Iterable} to evaluate; can be {@literal null}.
	 * @return {@literal true} if the given {@link Iterable} is empty,
	 * otherwise return {@literal false}.
	 * @see #nullSafeIterable(Iterable)
	 * @see java.lang.Iterable
	 */
	public static boolean isEmpty(@Nullable Iterable<?> iterable) {
		return !isNotEmpty(iterable);
	}

	/**
	 * Determines if the given {@link Iterable} is not empty.
	 *
	 * @param iterable {@link Iterable} to evaluate; can be {@literal null}.
	 * @return {@literal true} if the given {@link Iterable} is not empty,
	 * otherwise return {@literal false}.
	 * @see #nullSafeIterable(Iterable)
	 * @see java.lang.Iterable
	 */
	public static boolean isNotEmpty(@Nullable Iterable<?> iterable) {
		return nullSafeIterable(iterable).iterator().hasNext();
	}

	/**
	 * Protects against {@literal null} {@link Iterable} references.
	 *
	 * @param <T> {@link Class type} of elements contained by the {@link Iterable}.
	 * @param it {@link Iterable} to safe-guard from {@literal null}.
	 * @return the given {@link Iterable} if not {@literal null} or an empty {@link Iterable}.
	 * @see java.lang.Iterable
	 */
	@NonNull
	public static <T> Iterable<T> nullSafeIterable(@Nullable Iterable<T> it) {
		return it != null ? it : Collections::emptyIterator;
	}
}
