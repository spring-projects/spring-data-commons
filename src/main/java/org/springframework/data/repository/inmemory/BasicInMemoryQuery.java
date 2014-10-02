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
package org.springframework.data.repository.inmemory;

import org.springframework.data.domain.Sort;

/**
 * @author Christoph Strobl
 */
public abstract class BasicInMemoryQuery implements InMemoryQuery {

	private Sort sort;
	private int offset = -1;
	private int rows = -1;

	protected BasicInMemoryQuery() {

	}

	protected BasicInMemoryQuery(Sort sort) {
		this.sort = sort;
	}

	@Override
	public Object getCritieria() {
		return null;
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public int getRows() {
		return this.rows;
	}

	@Override
	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public void setRows(int rows) {
		this.rows = rows;
	}

	public BasicInMemoryQuery orderBy(Sort sort) {

		if (this.sort != null) {
			this.sort.and(sort);
		} else {
			this.sort = sort;
		}
		return this;
	}

	public BasicInMemoryQuery skip(int offset) {
		setOffset(offset);
		return this;
	}

	public BasicInMemoryQuery limit(int rows) {
		setRows(rows);
		return this;
	}

}
