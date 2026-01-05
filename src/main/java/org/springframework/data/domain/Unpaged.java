/*
 * Copyright 2017-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * {@link Pageable} implementation to represent the absence of pagination information.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
final class Unpaged implements Pageable {

	private static final Pageable UNSORTED = new Unpaged(Sort.unsorted());

	private final Sort sort;

	Unpaged(Sort sort) {
		this.sort = sort;
	}

	static Pageable sorted(Sort sort) {
		return sort.isSorted() ? new Unpaged(sort) : UNSORTED;
	}

	@Override
	public boolean isPaged() {
		return false;
	}

	@Override
	public Pageable previousOrFirst() {
		return this;
	}

	@Override
	public Pageable next() {
		return this;
	}

	@Override
	public boolean hasPrevious() {
		return false;
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public int getPageSize() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getPageNumber() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getOffset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pageable first() {
		return this;
	}

	@Override
	public Pageable withPage(int pageNumber) {

		if (pageNumber == 0) {
			return this;
		}

		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Unpaged unpaged)) {
			return false;
		}

		return sort.equals(unpaged.sort);
	}

	@Override
	public int hashCode() {
		return sort.hashCode();
	}
}
