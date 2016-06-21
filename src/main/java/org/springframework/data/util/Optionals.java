/*
 * Copyright 2016 the original author or authors.
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

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * Utility methods to work with {@link Optional}s.
 * 
 * @author Oliver Gierke
 */
@UtilityClass
public class Optionals {

	/**
	 * Returns whether any of the given {@link Optional}s is present.
	 * 
	 * @param optionals must not be {@literal null}.
	 * @return
	 */
	public static boolean isAnyPresent(Optional<?>... optionals) {

		Assert.notNull(optionals, "Optionals must not be null!");

		return Arrays.stream(optionals).anyMatch(Optional::isPresent);
	}

	/**
	 * Turns the given {@link Optional} into a one-element {@link Stream} or an empty one if not present.
	 * 
	 * @param optionals must not be {@literal null}.
	 * @return
	 */
	@SafeVarargs
	public static <T> Stream<T> toStream(Optional<? extends T>... optionals) {

		Assert.notNull(optionals, "Optional must not be null!");

		return Arrays.asList(optionals).stream().flatMap(it -> it.map(Stream::of).orElseGet(() -> Stream.empty()));
	}

	/**
	 * Applies the given function to the elements of the source and returns the first non-empty result.
	 * 
	 * @param source must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <S, T> Optional<T> firstNonEmpty(Iterable<S> source, Function<S, Optional<T>> function) {

		Assert.notNull(source, "Source must not be null!");
		Assert.notNull(function, "Function must not be null!");

		return Streamable.of(source).stream()//
				.map(it -> function.apply(it))//
				.filter(it -> it.isPresent())//
				.findFirst().orElseGet(() -> Optional.empty());
	}

	/**
	 * Applies the given function to the elements of the source and returns the first non-empty result.
	 * 
	 * @param source must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <S, T> T firstNonEmpty(Iterable<S> source, Function<S, T> function, T defaultValue) {

		Assert.notNull(source, "Source must not be null!");
		Assert.notNull(function, "Function must not be null!");

		return Streamable.of(source).stream()//
				.map(it -> function.apply(it))//
				.filter(it -> !it.equals(defaultValue))//
				.findFirst().orElse(defaultValue);
	}

	/**
	 * Returns the next element of the given {@link Iterator} or {@link Optional#empty()} in case there is no next
	 * element.
	 * 
	 * @param iterator must not be {@literal null}.
	 * @return
	 */
	public static <T> Optional<T> next(Iterator<T> iterator) {

		Assert.notNull(iterator, "Iterator must not be null!");

		return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
	}

	/**
	 * Returns a {@link Pair} if both {@link Optional} instances have values or {@link Optional#empty()} if one or both
	 * are missing.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static <T, S> Optional<Pair<T, S>> withBoth(Optional<T> left, Optional<S> right) {
		return left.flatMap(l -> right.map(r -> Pair.of(l, r)));
	}
}
