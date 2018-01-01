/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;

/**
 * Simple interface to ease streamability of {@link Iterable}s.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@FunctionalInterface
public interface Streamable<T> extends Iterable<T> {

	/**
	 * Returns an empty {@link Streamable}.
	 *
	 * @return will never be {@literal null}.
	 */
	static <T> Streamable<T> empty() {
		return Collections::emptyIterator;
	}

	/**
	 * Returns a {@link Streamable} with the given elements.
	 *
	 * @param t the elements to return.
	 * @return
	 */
	@SafeVarargs
	static <T> Streamable<T> of(T... t) {
		return () -> Arrays.asList(t).iterator();
	}

	/**
	 * Returns a {@link Streamable} for the given {@link Iterable}.
	 *
	 * @param iterable must not be {@literal null}.
	 * @return
	 */
	static <T> Streamable<T> of(Iterable<T> iterable) {

		Assert.notNull(iterable, "Iterable must not be null!");

		return iterable::iterator;
	}

	static <T> Streamable<T> of(Supplier<? extends Stream<T>> supplier) {
		return LazyStreamable.of(supplier);
	}

	/**
	 * Creates a non-parallel {@link Stream} of the underlying {@link Iterable}.
	 *
	 * @return will never be {@literal null}.
	 */
	default Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * Returns a new {@link Streamable} that will apply the given {@link Function} to the current one.
	 *
	 * @param mapper must not be {@literal null}.
	 * @return
	 * @see Stream#map(Function)
	 */
	default <R> Streamable<R> map(Function<? super T, ? extends R> mapper) {

		Assert.notNull(mapper, "Mapping function must not be null!");

		return Streamable.of(() -> stream().map(mapper));
	}

	/**
	 * Returns a new {@link Streamable} that will apply the given {@link Function} to the current one.
	 *
	 * @param mapper must not be {@literal null}.
	 * @return
	 * @see Stream#flatMap(Function)
	 */
	default <R> Streamable<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {

		Assert.notNull(mapper, "Mapping function must not be null!");

		return Streamable.of(() -> stream().flatMap(mapper));
	}

	/**
	 * Returns a new {@link Streamable} that will apply the given filter {@link Predicate} to the current one.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return
	 * @see Stream#filter(Predicate)
	 */
	default Streamable<T> filter(Predicate<? super T> predicate) {

		Assert.notNull(predicate, "Filter predicate must not be null!");

		return Streamable.of(() -> stream().filter(predicate));
	}
}
