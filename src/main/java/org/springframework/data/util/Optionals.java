/*
 * Copyright 2016-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.util.Assert;

/**
 * Utility methods to work with {@link Optional}s.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public interface Optionals {

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

		return Arrays.asList(optionals).stream().flatMap(it -> it.map(Stream::of).orElseGet(Stream::empty));
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
				.map(function::apply)//
				.filter(Optional::isPresent)//
				.findFirst().orElseGet(Optional::empty);
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
				.map(function::apply)//
				.filter(it -> !it.equals(defaultValue))//
				.findFirst().orElse(defaultValue);
	}

	/**
	 * Invokes the given {@link Supplier}s for {@link Optional} results one by one and returns the first non-empty one.
	 *
	 * @param suppliers must not be {@literal null}.
	 * @return
	 */
	@SafeVarargs
	public static <T> Optional<T> firstNonEmpty(Supplier<Optional<T>>... suppliers) {

		Assert.notNull(suppliers, "Suppliers must not be null!");

		return firstNonEmpty(Streamable.of(suppliers));
	}

	/**
	 * Invokes the given {@link Supplier}s for {@link Optional} results one by one and returns the first non-empty one.
	 *
	 * @param suppliers must not be {@literal null}.
	 * @return
	 */
	public static <T> Optional<T> firstNonEmpty(Iterable<Supplier<Optional<T>>> suppliers) {

		Assert.notNull(suppliers, "Suppliers must not be null!");

		return Streamable.of(suppliers).stream()//
				.map(Supplier::get)//
				.filter(Optional::isPresent)//
				.findFirst().orElse(Optional.empty());
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

	/**
	 * Invokes the given {@link BiConsumer} if all given {@link Optional} are present.
	 *
	 * @param left must not be {@literal null}.
	 * @param right must not be {@literal null}.
	 * @param consumer must not be {@literal null}.
	 */
	public static <T, S> void ifAllPresent(Optional<T> left, Optional<S> right, BiConsumer<T, S> consumer) {

		Assert.notNull(left, "Optional must not be null!");
		Assert.notNull(right, "Optional must not be null!");
		Assert.notNull(consumer, "Consumer must not be null!");

		mapIfAllPresent(left, right, (l, r) -> {
			consumer.accept(l, r);
			return null;
		});
	}

	/**
	 * Maps the values contained in the given {@link Optional} if both of them are present.
	 *
	 * @param left must not be {@literal null}.
	 * @param right must not be {@literal null}.
	 * @param function must not be {@literal null}.
	 * @return
	 */
	public static <T, S, R> Optional<R> mapIfAllPresent(Optional<T> left, Optional<S> right,
			BiFunction<T, S, R> function) {

		Assert.notNull(left, "Optional must not be null!");
		Assert.notNull(right, "Optional must not be null!");
		Assert.notNull(function, "BiFunctionmust not be null!");

		return left.flatMap(l -> right.map(r -> function.apply(l, r)));
	}

	/**
	 * Invokes the given {@link Consumer} if the {@link Optional} is present or the {@link Runnable} if not.
	 *
	 * @param optional must not be {@literal null}.
	 * @param consumer must not be {@literal null}.
	 * @param runnable must not be {@literal null}.
	 */
	public static <T> void ifPresentOrElse(Optional<T> optional, Consumer<? super T> consumer, Runnable runnable) {

		Assert.notNull(optional, "Optional must not be null!");
		Assert.notNull(consumer, "Consumer must not be null!");
		Assert.notNull(runnable, "Runnable must not be null!");

		if (optional.isPresent()) {
			optional.ifPresent(consumer);
		} else {
			runnable.run();
		}
	}
}
