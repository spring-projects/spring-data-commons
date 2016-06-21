/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.data.domain;

import java.util.Optional;

import org.springframework.data.domain.Sort.Direction;

/**
 * Basic Java Bean implementation of {@code Pageable}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class PageRequest extends AbstractPageRequest {

	private static final long serialVersionUID = -4541509938956089562L;

	private final Sort sort;

	/**
	 * Creates a new {@link PageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return the first
	 * page.
	 * 
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @deprecated use {@link #of(int, int)} instead.
	 */
	@Deprecated
	public PageRequest(int page, int size) {
		this(page, size, Sort.unsorted());
	}

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 * 
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param direction the direction of the {@link Sort} to be specified, can be {@literal null}.
	 * @param properties the properties to sort by, must not be {@literal null} or empty.
	 * @deprecated use {@link #of(int, int, Direction, String...)} instead.
	 */
	@Deprecated
	public PageRequest(int page, int size, Direction direction, String... properties) {
		this(page, size, new Sort(direction, properties));
	}

	/**
	 * Creates a new {@link PageRequest} with sort parameters applied.
	 * 
	 * @param page zero-based page index.
	 * @param size the size of the page to be returned.
	 * @param sort can be {@literal null}.
	 * @deprecated use {@link #of(int, int, Optional)} instead.
	 */
	@Deprecated
	public PageRequest(int page, int size, Sort sort) {

		super(page, size);

		this.sort = sort;
	}

	public static PageRequest of(int page, int site) {
		return of(page, site, Sort.unsorted());
	}

	public static PageRequest of(int page, int site, Sort sort) {
		return new PageRequest(page, site, sort);
	}

	public static PageRequest of(int page, int size, Direction direction, String... properties) {
		return of(page, size, new Sort(direction, properties));
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
	public Pageable next() {
		return new PageRequest(getPageNumber() + 1, getPageSize(), getSort());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.AbstractPageRequest#previous()
	 */
	public PageRequest previous() {
		return getPageNumber() == 0 ? this : new PageRequest(getPageNumber() - 1, getPageSize(), getSort());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#first()
	 */
	public Pageable first() {
		return new PageRequest(0, getPageSize(), getSort());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof PageRequest)) {
			return false;
		}

		PageRequest that = (PageRequest) obj;

		return super.equals(that) && this.sort.equals(that.sort);
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
