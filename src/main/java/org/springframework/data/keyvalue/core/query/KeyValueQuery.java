/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue.core.query;

import org.springframework.data.domain.Sort;

/**
 * @author Christoph Strobl
 * @since 1.10
 * @param <T> Criteria type
 */
public class KeyValueQuery<T> {

	private Sort sort;
	private int offset = -1;
	private int rows = -1;
	private T criteria;

	/**
	 * Creates new instance of {@link KeyValueQuery}.
	 */
	public KeyValueQuery() {}

	/**
	 * Creates new instance of {@link KeyValueQuery} with given criteria.
	 * 
	 * @param criteria can be {@literal null}.
	 */
	public KeyValueQuery(T criteria) {
		this.criteria = criteria;
	}

	/**
	 * Creates new instance of {@link KeyValueQuery} with given {@link Sort}.
	 * 
	 * @param sort can be {@literal null}.
	 */
	public KeyValueQuery(Sort sort) {
		this.sort = sort;
	}

	/**
	 * Get the criteria object.
	 * 
	 * @return
	 */
	public T getCritieria() {
		return criteria;
	}

	/**
	 * Get {@link Sort}.
	 * 
	 * @return
	 */
	public Sort getSort() {
		return sort;
	}

	/**
	 * Number of elements to skip.
	 * 
	 * @return negative value if not set.
	 */
	public int getOffset() {
		return this.offset;
	}

	/**
	 * Number of elements to read.
	 * 
	 * @return negative value if not set.
	 */
	public int getRows() {
		return this.rows;
	}

	/**
	 * Set the number of elements to skip.
	 * 
	 * @param offset use negative value for none.
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * Set the number of elements to read.
	 * 
	 * @param offset use negative value for all.
	 */
	public void setRows(int rows) {
		this.rows = rows;
	}

	/**
	 * Set {@link Sort} to be applied.
	 * 
	 * @param sort
	 */
	public void setSort(Sort sort) {
		this.sort = sort;
	}

	/**
	 * Add given {@link Sort}.
	 * 
	 * @param sort {@literal null} {@link Sort} will be ignored.
	 * @return
	 */
	public KeyValueQuery<T> orderBy(Sort sort) {

		if (sort == null) {
			return this;
		}

		if (this.sort != null) {
			this.sort.and(sort);
		} else {
			this.sort = sort;
		}
		return this;
	}

	/**
	 * @see KeyValueQuery#setOffset(int)
	 * @param offset
	 * @return
	 */
	public KeyValueQuery<T> skip(int offset) {
		setOffset(offset);
		return this;
	}

	/**
	 * @see KeyValueQuery#setRows(int)
	 * @param rows
	 * @return
	 */
	public KeyValueQuery<T> limit(int rows) {
		setRows(rows);
		return this;
	}

}
