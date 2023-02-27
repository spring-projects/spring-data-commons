/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.domain;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.springframework.data.util.Streamable;

/**
 * A scroll of data consumed from an underlying query result. A scroll is similar to {@link Slice} in the sense that it
 * contains a subset of the actual query results for easier consumption of large result sets. The scroll is less
 * opinionated about the actual data retrieval, whether the query has used index/offset, keyset-based pagination or
 * cursor resume tokens.
 *
 * @author Mark Paluch
 * @since 3.1
 * @see ScrollPosition
 */
public interface Scroll<T> extends Streamable<T> {

	/**
	 * Construct a {@link Scroll}.
	 *
	 * @param items the list of data.
	 * @param positionFunction the list of data.
	 * @return the {@link Scroll}.
	 * @param <T>
	 */
	static <T> Scroll<T> from(List<T> items, IntFunction<? extends ScrollPosition> positionFunction) {
		return new ScrollImpl<>(items, positionFunction, false);
	}

	/**
	 * Construct a {@link Scroll}.
	 *
	 * @param items the list of data.
	 * @param positionFunction the list of data.
	 * @param hasNext
	 * @return the {@link Scroll}.
	 * @param <T>
	 */
	static <T> Scroll<T> from(List<T> items, IntFunction<? extends ScrollPosition> positionFunction, boolean hasNext) {
		return new ScrollImpl<>(items, positionFunction, hasNext);
	}

	/**
	 * Returns the number of elements in this scroll.
	 *
	 * @return the number of elements in this scroll.
	 */
	int size();

	/**
	 * Returns {@code true} if this scroll contains no elements.
	 *
	 * @return {@code true} if this scroll contains no elements
	 */
	boolean isEmpty();

	/**
	 * Returns the scroll content as {@link List}.
	 *
	 * @return
	 */
	List<T> getContent();

	/**
	 * Returns whether the current scroll is the last one.
	 *
	 * @return
	 */
	default boolean isLast() {
		return !hasNext();
	}

	/**
	 * Returns if there is a next scroll.
	 *
	 * @return if there is a next scroll window.
	 */
	boolean hasNext();

	/**
	 * Returns the {@link ScrollPosition} at {@code index}.
	 *
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
	 */
	ScrollPosition positionAt(int index);

	/**
	 * Returns the {@link ScrollPosition} for {@code object}.
	 *
	 * @param object
	 * @return
	 * @throws NoSuchElementException if the object is not part of the result.
	 */
	default ScrollPosition positionAt(T object) {

		int index = getContent().indexOf(object);

		if (index == -1) {
			throw new NoSuchElementException();
		}

		return positionAt(index);
	}

	// TODO: First and last seem to conflict with first/last scroll or first/last position of elements.
	// these methods should rather express the position of the first element within this scroll and the scroll position
	// to be used to get the next Scroll.
	/**
	 * Returns the first {@link ScrollPosition} or throw {@link NoSuchElementException} if the list is empty.
	 *
	 * @return the first {@link ScrollPosition}.
	 * @throws NoSuchElementException if this result is empty.
	 */
	default ScrollPosition firstPosition() {

		if (size() == 0) {
			throw new NoSuchElementException();
		}

		return positionAt(0);
	}

	/**
	 * Returns the last {@link ScrollPosition} or throw {@link NoSuchElementException} if the list is empty.
	 *
	 * @return the last {@link ScrollPosition}.
	 * @throws NoSuchElementException if this result is empty.
	 */
	default ScrollPosition lastPosition() {

		if (size() == 0) {
			throw new NoSuchElementException();
		}

		return positionAt(size() - 1);
	}

	/**
	 * Returns a new {@link Scroll} with the content of the current one mapped by the given {@code converter}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link Scroll} with the content of the current one mapped by the given {@code converter}.
	 */
	<U> Scroll<U> map(Function<? super T, ? extends U> converter);

}
