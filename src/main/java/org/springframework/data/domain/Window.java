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
 * A set of data consumed from an underlying query result. A {@link Window} is similar to {@link Slice} in the sense
 * that it contains a subset of the actual query results for easier scrolling across large result sets. The window is
 * less opinionated about the actual data retrieval, whether the query has used index/offset, keyset-based pagination or
 * cursor resume tokens.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.1
 * @see ScrollPosition
 */
public interface Window<T> extends Streamable<T> {

	/**
	 * Construct a {@link Window}.
	 *
	 * @param items the list of data.
	 * @param positionFunction the list of data.
	 * @return the {@link Window}.
	 * @param <T>
	 */
	static <T> Window<T> from(List<T> items, IntFunction<? extends ScrollPosition> positionFunction) {
		return new WindowImpl<>(items, positionFunction, false);
	}

	/**
	 * Construct a {@link Window}.
	 *
	 * @param items the list of data.
	 * @param positionFunction the list of data.
	 * @param hasNext
	 * @return the {@link Window}.
	 * @param <T>
	 */
	static <T> Window<T> from(List<T> items, IntFunction<? extends ScrollPosition> positionFunction, boolean hasNext) {
		return new WindowImpl<>(items, positionFunction, hasNext);
	}

	/**
	 * Returns the number of elements in this window.
	 *
	 * @return the number of elements in this window.
	 */
	int size();

	/**
	 * Returns {@code true} if this window contains no elements.
	 *
	 * @return {@code true} if this window contains no elements
	 */
	boolean isEmpty();

	/**
	 * Returns the windows content as {@link List}.
	 *
	 * @return
	 */
	List<T> getContent();

	/**
	 * Returns whether the current window is the last one.
	 *
	 * @return
	 */
	default boolean isLast() {
		return !hasNext();
	}

	/**
	 * Returns if there is a next window.
	 *
	 * @return if there is a next window.
	 */
	boolean hasNext();

	/**
	 * Returns whether the underlying scroll mechanism can provide a {@link ScrollPosition} at {@code index}.
	 *
	 * @param index
	 * @return {@code true} if a {@link ScrollPosition} can be created; {@code false} otherwise.
	 * @see #positionAt(int)
	 */
	default boolean hasPosition(int index) {
		try {
			return positionAt(index) != null;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	/**
	 * Returns the {@link ScrollPosition} at {@code index}.
	 *
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException if the index is out of range ({@code index < 0 || index >= size()}).
	 * @throws IllegalStateException if the underlying scroll mechanism cannot provide a scroll position for the given
	 *           object.
	 */
	ScrollPosition positionAt(int index);

	/**
	 * Returns the {@link ScrollPosition} for {@code object}.
	 *
	 * @param object
	 * @return
	 * @throws NoSuchElementException if the object is not part of the result.
	 * @throws IllegalStateException if the underlying scroll mechanism cannot provide a scroll position for the given
	 *           object.
	 */
	default ScrollPosition positionAt(T object) {

		int index = getContent().indexOf(object);

		if (index == -1) {
			throw new NoSuchElementException();
		}

		return positionAt(index);
	}

	/**
	 * Returns a new {@link Window} with the content of the current one mapped by the given {@code converter}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link Window} with the content of the current one mapped by the given {@code converter}.
	 */
	<U> Window<U> map(Function<? super T, ? extends U> converter);

}
