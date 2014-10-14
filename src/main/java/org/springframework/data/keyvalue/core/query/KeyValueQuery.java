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
 * @param <T>
 */
public class KeyValueQuery<T> {

	private Sort sort;
	private int offset = -1;
	private int rows = -1;
	private T criteria;

	public KeyValueQuery() {}

	public KeyValueQuery(T criteria) {
		this.criteria = criteria;
	}

	public KeyValueQuery(Sort sort) {
		this.sort = sort;
	}

	public T getCritieria() {
		return criteria;
	}

	public Sort getSort() {
		return sort;
	}

	public int getOffset() {
		return this.offset;
	}

	public int getRows() {
		return this.rows;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public KeyValueQuery<T> orderBy(Sort sort) {

		if (this.sort != null) {
			this.sort.and(sort);
		} else {
			this.sort = sort;
		}
		return this;
	}

	public KeyValueQuery<T> skip(int offset) {
		setOffset(offset);
		return this;
	}

	public KeyValueQuery<T> limit(int rows) {
		setRows(rows);
		return this;
	}

}
