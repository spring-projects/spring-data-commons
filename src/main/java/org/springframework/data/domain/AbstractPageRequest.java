/*
 * Copyright 2013-2025 the original author or authors.
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

import java.io.Serial;
import java.io.Serializable;

import org.springframework.util.ObjectUtils;

/**
 * Abstract Java Bean implementation of {@code Pageable}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Alex Bondarev
 * @author Johannes Englmeier
 * @author Thach Le
 */
public abstract class AbstractPageRequest implements Pageable, Serializable {

	private static final @Serial long serialVersionUID = 1232825578694716871L;

	private final int pageNumber;
	private final int pageSize;

	/**
	 * Creates a new {@link AbstractPageRequest}. Pages are zero indexed, thus providing 0 for {@code pageNumber} will
	 * return the first pageNumber.
	 *
	 * @param pageNumber zero-based page number, must not be negative.
	 * @param pageSize the size of the page to be returned, must be greater than 0.
	 */
	public AbstractPageRequest(int pageNumber, int pageSize) {

		if (pageNumber < 0) {
			throw new IllegalArgumentException("Page index must not be less than zero");
		}

		if (pageSize < 1) {
			throw new IllegalArgumentException("Page size must not be less than one");
		}

		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
	}

	@Override
	public int getPageSize() {
		return pageSize;
	}

	@Override
	public int getPageNumber() {
		return pageNumber;
	}

	@Override
	public long getOffset() {
		return (long) pageNumber * (long) pageSize;
	}

	@Override
	public boolean hasPrevious() {
		return pageNumber > 0;
	}

	@Override
	public Pageable previousOrFirst() {
		return hasPrevious() ? previous() : first();
	}

	@Override
	public abstract Pageable next();

	/**
	 * Returns the {@link Pageable} requesting the previous {@link Page}.
	 *
	 * @return the previous {@link Pageable}
	 */
	public abstract Pageable previous();

	@Override
	public abstract Pageable first();

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AbstractPageRequest that)) {
			return false;
		}
		if (pageNumber != that.pageNumber) {
			return false;
		}
		return pageSize == that.pageSize;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(pageNumber, pageSize);
	}
}
