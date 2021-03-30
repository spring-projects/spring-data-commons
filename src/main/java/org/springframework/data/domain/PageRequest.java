/*
 * Copyright 2008-2021 the original author or authors.
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

import org.springframework.data.domain.Sort.Direction;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Basic Java Bean implementation of {@link Pageable}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Anastasiia Smirnova
 * @author Mark Paluch
 */
public class PageRequest extends AbstractPageRequest {

	private static final long serialVersionUID = -4541509938956089562L;

	private final Sort sort;

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 *
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
	 */
	protected PageRequest(int page, int size, Sort sort) {

		super(page, size);

		Assert.notNull(sort, "Sort must not be null!");

		this.sort = sort;
	}

	/**
	 * Creates a new unsorted {@link PageRequest}.
	 *
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size) {
		return of(page, size, Sort.unsorted());
	}

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 *
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param sort must not be {@literal null}, use {@link Sort#unsorted()} instead.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size, Sort sort) {
		return new PageRequest(page, size, sort);
	}

	/**
	 * Creates a new {@link PageRequest} with sort direction and properties applied.
	 *
	 * @param page zero-based page index, must not be negative.
	 * @param size the size of the page to be returned, must be greater than 0.
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @since 2.0
	 */
	public static PageRequest of(int page, int size, Direction direction, String... properties) {
		return of(page, size, Sort.by(direction, properties));
	}

	/**
	 * Creates a new {@link PageRequest} for the first page (page number {@code 0}) given {@code pageSize} .
	 *
	 * @param pageSize the size of the page to be returned, must be greater than 0.
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	public static PageRequest ofSize(int pageSize) {
		return PageRequest.of(0, pageSize);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getSort()
	 */
	public Sort getSort() {
		return sort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#next()
	 */
	@Override
	public PageRequest next() {
		return new PageRequest(getPageNumber() + 1, getPageSize(), getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequest#previous()
	 */
	@Override
	public PageRequest previous() {
		return getPageNumber() == 0 ? this : new PageRequest(getPageNumber() - 1, getPageSize(), getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#first()
	 */
	@Override
	public PageRequest first() {
		return new PageRequest(0, getPageSize(), getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof PageRequest)) {
			return false;
		}

		PageRequest that = (PageRequest) obj;

		return super.equals(that) && this.sort.equals(that.sort);
	}

	/**
	 * Creates a new {@link PageRequest} with {@code pageNumber} applied.
	 *
	 * @param pageNumber
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	@Override
	public PageRequest withPage(int pageNumber) {
		return new PageRequest(pageNumber, getPageSize(), getSort());
	}

	/**
	 * Creates a new {@link PageRequest} with {@link Direction} and {@code properties} applied.
	 *
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	public PageRequest withSort(Direction direction, String... properties) {
		return new PageRequest(getPageNumber(), getPageSize(), Sort.by(direction, properties));
	}

	/**
	 * Creates a new {@link PageRequest} with {@link Sort} applied.
	 *
	 * @param sort must not be {@literal null}.
	 * @return a new {@link PageRequest}.
	 * @since 2.5
	 */
	public PageRequest withSort(Sort sort) {
		return new PageRequest(getPageNumber(), getPageSize(), sort);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 31 * super.hashCode() + sort.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Page request [number: %d, size %d, sort: %s]", getPageNumber(), getPageSize(), sort);
	}

}
