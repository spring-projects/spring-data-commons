/*
 * Copyright 2008-2013 the original author or authors.
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

/**
 * Abstract interface for pagination information.
 * 
 * @author Oliver Gierke
 */
public interface Pageable {

	/**
	 * Returns the page to be returned.
	 * 
	 * @return the page to be returned.
	 */
	int getPageNumber();

	/**
	 * Returns the number of items to be returned.
	 * 
	 * @return the number of items of that page
	 */
	int getPageSize();

	/**
	 * Returns the offset to be taken according to the underlying page and page size.
	 * 
	 * @return the offset to be taken
	 */
	long getOffset();

	/**
	 * Returns the sorting parameters.
	 * 
	 * @return
	 */
	Sort getSort();

	default Sort getSortOr(Sort sort) {
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
	 * Returns whether there's a previous {@link Pageable} we can access from the current one. Will return
	 * {@literal false} in case the current {@link Pageable} already refers to the first page.
	 * 
	 * @return
	 */
	boolean hasPrevious();

	public static Pageable NONE = new Pageable() {

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
			return Sort.unsorted();
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
	};
}
