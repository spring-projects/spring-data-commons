/*
 * Copyright 2015-2017 the original author or authors.
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

import static java.util.stream.Collectors.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Spring Data specific Java {@link Stream} utility methods and classes.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.10
 */
public interface StreamUtils {

	/**
	 * Returns a {@link Stream} backed by the given {@link Iterator}
	 * 
	 * @param iterator must not be {@literal null}.
	 * @return
	 */
	public static <T> Stream<T> createStreamFromIterator(Iterator<T> iterator) {

		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, Spliterator.NONNULL);
		return StreamSupport.stream(spliterator, false);
	}

	/**
	 * Returns a {@link Stream} backed by the given {@link CloseableIterator} and forwarding calls to
	 * {@link Stream#close()} to the iterator.
	 * 
	 * @param iterator must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static <T> Stream<T> createStreamFromIterator(CloseableIterator<T> iterator) {

		Assert.notNull(iterator, "Iterator must not be null!");

		return createStreamFromIterator((Iterator<T>) iterator).onClose(() -> iterator.close());
	}

	/**
	 * Returns a {@link Collector} to create an unmodifiable {@link List}.
	 * 
	 * @return will never be {@literal null}.
	 */
	public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
		return collectingAndThen(toList(), Collections::unmodifiableList);
	}

	/**
	 * Returns a {@link Collector} to create an unmodifiable {@link Set}.
	 * 
	 * @return will never be {@literal null}.
	 */
	public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
		return collectingAndThen(toSet(), Collections::unmodifiableSet);
	}

	/**
	 * Returns a {@link Collector} to create a {@link MultiValueMap}.
	 *
	 * @param keyFunction {@link Function} to create a key from an element of the {@link java.util.stream.Stream}
	 * @param valueFunction {@link Function} to create a value from an element of the {@link java.util.stream.Stream}
	 */
	public static <T, K, V> Collector<T, MultiValueMap<K, V>, MultiValueMap<K, V>> toMultiMap(Function<T, K> keyFunction,
			Function<T, V> valueFunction) {
		return MultiValueMapCollector.of(keyFunction, valueFunction);
	}
}
