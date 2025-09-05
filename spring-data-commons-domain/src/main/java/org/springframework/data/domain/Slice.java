/*
 * Copyright 2014-2025 the original author or authors.
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
import java.util.function.Function;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.util.Streamable;

/**
 * A slice of data that indicates whether there's a next or previous slice available. Allows to obtain a
 * {@link Pageable} to request a previous or next {@link Slice}.
 *
 * @author Oliver Gierke
 * @since 1.8
 */
public interface Slice<T> extends Streamable<T> {

	/**
	 * Returns the number of the current {@link Slice}. Is always non-negative.
	 *
	 * @return the number of the current {@link Slice}.
	 */
	int getNumber();

	/**
	 * Returns the size of the {@link Slice}.
	 *
	 * @return the size of the {@link Slice}.
	 */
	int getSize();

	/**
	 * Returns the number of elements currently on this {@link Slice}.
	 *
	 * @return the number of elements currently on this {@link Slice}.
	 */
	int getNumberOfElements();

	/**
	 * Returns the page content as {@link List}.
	 *
	 * @return the page content as {@link List}.
	 */
	List<T> getContent();

	/**
	 * Returns whether the {@link Slice} has content at all.
	 *
	 * @return {@literal true} if the {@link Slice} has content at all.
	 */
	boolean hasContent();

	/**
	 * Returns the sorting parameters for the {@link Slice}.
	 *
	 * @return the sorting parameters for the {@link Slice}.
	 */
	Sort getSort();

	/**
	 * Returns whether the current {@link Slice} is the first one.
	 *
	 * @return {@literal true} if the current {@link Slice} is the first one.
	 */
	boolean isFirst();

	/**
	 * Returns whether the current {@link Slice} is the last one.
	 *
	 * @return {@literal true} if the current {@link Slice} is the last one.
	 */
	boolean isLast();

	/**
	 * Returns if there is a next {@link Slice}.
	 *
	 * @return if there is a next {@link Slice}.
	 */
	boolean hasNext();

	/**
	 * Returns if there is a previous {@link Slice}.
	 *
	 * @return if there is a previous {@link Slice}.
	 */
	boolean hasPrevious();

	/**
	 * Returns the {@link Pageable} that's been used to request the current {@link Slice}.
	 *
	 * @return the {@link Pageable} that's been used to request the current {@link Slice}.
	 * @since 2.0
	 */
	default Pageable getPageable() {
		return PageRequest.of(getNumber(), getSize(), getSort());
	}

	/**
	 * Returns the {@link Pageable} to request the next {@link Slice}. Can be {@link Pageable#unpaged()} in case the
	 * current {@link Slice} is already the last one. Clients should check {@link #hasNext()} before calling this method.
	 *
	 * @return the {@link Pageable} to request the next {@link Slice}.
	 * @see #nextOrLastPageable()
	 */
	Pageable nextPageable();

	/**
	 * Returns the {@link Pageable} to request the previous {@link Slice}. Can be {@link Pageable#unpaged()} in case the
	 * current {@link Slice} is already the first one. Clients should check {@link #hasPrevious()} before calling this
	 * method.
	 *
	 * @return the {@link Pageable} to request the previous {@link Slice}.
	 * @see #previousPageable()
	 */
	Pageable previousPageable();

	/**
	 * Returns the {@link Pageable} describing the next slice or the one describing the current slice in case it's the
	 * last one.
	 *
	 * @return the {@link Pageable} describing the next slice or the one describing the current slice in case it's the
	 *         last one
	 * @since 2.2
	 */
	default Pageable nextOrLastPageable() {
		return hasNext() ? nextPageable() : getPageable();
	}

	/**
	 * Returns the {@link Pageable} describing the previous slice or the one describing the current slice in case it's the
	 * first one.
	 *
	 * @return the {@link Pageable} describing the previous slice or the one describing the current slice in case it's the
	 *         first one.
	 * @since 2.2
	 */
	default Pageable previousOrFirstPageable() {
		return hasPrevious() ? previousPageable() : getPageable();
	}

	/**
	 * Returns a new {@link Slice} with the content of the current one mapped by the given {@link Converter}.
	 *
	 * @param converter must not be {@literal null}.
	 * @return a new {@link Slice} with the content of the current one mapped by the given {@link Converter}.
	 * @since 1.10
	 */
	@Override
	<U> Slice<U> map(Function<? super T, ? extends U> converter);

}
