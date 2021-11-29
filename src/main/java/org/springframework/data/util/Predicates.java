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
package org.springframework.data.util;

import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Utility methods to work with {@link Predicate}s.
 *
 * @author Mark Paluch
 * @since 2.7
 */
public interface Predicates {

	/**
	 * A {@link Predicate} that yields always {@code true}.
	 *
	 * @return a {@link Predicate} that yields always {@code true}.
	 */
	static <T> Predicate<T> isTrue() {
		return t -> true;
	}

	/**
	 * A {@link Predicate} that yields always {@code false}.
	 *
	 * @return a {@link Predicate} that yields always {@code false}.
	 */
	static <T> Predicate<T> isFalse() {
		return t -> false;
	}

	/**
	 * Returns a {@link Predicate} that represents the logical negation of {@code predicate}.
	 *
	 * @return a {@link Predicate} that represents the logical negation of {@code predicate}.
	 */
	static <T> Predicate<T> negate(Predicate<T> predicate) {

		Assert.notNull(predicate, "Predicate must not be null!");
		return predicate.negate();
	}
}
