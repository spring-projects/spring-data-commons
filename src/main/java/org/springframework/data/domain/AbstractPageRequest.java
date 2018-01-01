/*
 * Copyright 2013-2018 the original author or authors.
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

import java.io.Serializable;

import org.springframework.lang.Nullable;

/**
 * Abstract Java Bean implementation of {@code Pageable}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public abstract class AbstractPageRequest implements Pageable, Serializable {

	private static final long serialVersionUID = 1232825578694716871L;

	private final int page;
	private final int size;

	/**
	 * Creates a new {@link AbstractPageRequest}. Pages are zero indexed, thus providing 0 for {@code page} will return
	 * the first page.
	 *
	 * @param page must not be less than zero.
	 * @param size must not be less than one.
	 */
	public AbstractPageRequest(int page, int size) {

		if (page < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero!");
		}

		if (size < 1) {
			throw new IllegalArgumentException("Page size must not be less than one!");
		}

		this.page = page;
		this.size = size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageSize()
	 */
	public int getPageSize() {
		return size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getPageNumber()
	 */
	public int getPageNumber() {
		return page;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#getOffset()
	 */
	public long getOffset() {
		return page * size;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#hasPrevious()
	 */
	public boolean hasPrevious() {
		return page > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#previousOrFirst()
	 */
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#next()
	 */
	public abstract Pageable next();

	/**
	 * Returns the {@link Pageable} requesting the previous {@link Page}.
	 *
	 * @return
	 */
	public abstract Pageable previous();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.Pageable#first()
	 */
	public abstract Pageable first();

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;

		result = prime * result + page;
		result = prime * result + size;

		return result;
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

		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		AbstractPageRequest other = (AbstractPageRequest) obj;
		return this.page == other.page && this.size == other.size;
	}
}
