/*
 * Copyright 2008-2023 the original author or authors.
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

import java.util.Optional;

import org.springframework.util.Assert;

/**
 * Abstract interface for pagination information.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface Pageable {

	/**
	 * Returns a {@link Pageable} instance representing no pagination setup.
	 *
	 * @return
	 */
	static Pageable unpaged() {
		return Unpaged.INSTANCE;
	}

	/**
	 * Creates a new {@link Pageable} for the first page (page number {@code 0}) given {@code pageSize} .
	 *
	 * @param pageSize the size of the page to be returned, must be greater than 0.
	 * @return a new {@link Pageable}.
	 * @since 2.5
	 */
	static Pageable ofSize(int pageSize) {
		return PageRequest.of(0, pageSize);
	}

	/**
	 * Returns whether the current {@link Pageable} contains pagination information.
	 *
	 * @return
	 */
	default boolean isPaged() {
		return true;
	}

	/**
	 * Returns whether the current {@link Pageable} does not contain pagination information.
	 *
	 * @return
	 */
	default boolean isUnpaged() {
		return !isPaged();
	}

	/**
	 * Returns the page to be returned.
	 *
	 * @return the page to be returned or throws {@link UnsupportedOperationException} if the object is
	 *         {@link #isUnpaged()}.
	 * @throws UnsupportedOperationException if the object is {@link #isUnpaged()}.
	 */
	int getPageNumber();

	/**
	 * Returns the number of items to be returned.
	 *
	 * @return the number of items of that page or throws {@link UnsupportedOperationException} if the object is
	 *         {@link #isUnpaged()}.
	 * @throws UnsupportedOperationException if the object is {@link #isUnpaged()}.
	 */
	int getPageSize();

	/**
	 * Returns the offset to be taken according to the underlying page and page size.
	 *
	 * @return the offset to be taken or throws {@link UnsupportedOperationException} if the object is
	 *         {@link #isUnpaged()}.
	 * @throws UnsupportedOperationException if the object is {@link #isUnpaged()}.
	 */
	long getOffset();

	/**
	 * Returns the sorting parameters.
	 *
	 * @return
	 */
	Sort getSort();

	/**
	 * Returns the current {@link Sort} or the given one if the current one is unsorted.
	 *
	 * @param sort must not be {@literal null}.
	 * @return
	 */
	default Sort getSortOr(Sort sort) {

		Assert.notNull(sort, "Fallback Sort must not be null");

		return getSort().isSorted() ? getSort() : sort;
	}

	/**
	 * Returns the {@link Pageable} requesting the next {@link Page}.
	 *
	 * @return
	 */
	Pageable next();

	/**
	 * Returns the previous {@link Pageable} or the first {@link Pageable} if the current one already is the first one.
	 *
	 * @return
	 */
	Pageable previousOrFirst();

	/**
	 * Returns the {@link Pageable} requesting the first page.
	 *
	 * @return
	 */
	Pageable first();

	/**
	 * Creates a new {@link Pageable} with {@code pageNumber} applied.
	 *
	 * @param pageNumber
	 * @return a new {@link PageRequest} or throws {@link UnsupportedOperationException} if the object is
	 *         {@link #isUnpaged()} and the {@code pageNumber} is not zero.
	 * @since 2.5
	 * @throws UnsupportedOperationException if the object is {@link #isUnpaged()}.
	 */
	Pageable withPage(int pageNumber);

	/**
	 * Returns whether there's a previous {@link Pageable} we can access from the current one. Will return
	 * {@literal false} in case the current {@link Pageable} already refers to the first page.
	 *
	 * @return
	 */
	boolean hasPrevious();

	/**
	 * Returns an {@link Optional} so that it can easily be mapped on.
	 *
	 * @return
	 */
	default Optional<Pageable> toOptional() {
		return isUnpaged() ? Optional.empty() : Optional.of(this);
	}

	/**
	 * Returns an {@link OffsetScrollPosition} from this pageable if the page request {@link #isPaged() is paged}.
	 *
	 * @return
	 * @throws IllegalStateException if the request is {@link #isUnpaged()}
	 * @since 3.1
	 */
	default OffsetScrollPosition toScrollPosition() {

		if (isUnpaged()) {
			throw new IllegalStateException("Cannot create OffsetScrollPosition from an unpaged instance");
		}

		return ScrollPosition.offset(getOffset());
	}
}
