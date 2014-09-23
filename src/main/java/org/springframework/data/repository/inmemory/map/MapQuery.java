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
package org.springframework.data.repository.inmemory.map;

import java.util.Comparator;

import org.springframework.data.repository.inmemory.InMemoryQuery;
import org.springframework.expression.Expression;

/**
 * {@link InMemoryQuery} implementation to be used for executing {@link Expression} based queries.
 * 
 * @author Christoph Strobl
 */
public class MapQuery implements InMemoryQuery {

	private final Expression criteria;
	private Comparator<?> sort;
	private int offset = -1;
	private int rows = -1;

	public MapQuery(Expression criteria) {
		this.criteria = criteria;
	}

	public MapQuery orderBy(Comparator<?> comparator) {
		this.sort = comparator;
		return this;
	}

	public MapQuery skip(int offset) {
		this.offset = offset;
		return this;
	}

	public MapQuery limit(int rows) {
		this.rows = rows;
		return this;
	}

	@Override
	public Expression getCritieria() {
		return this.criteria;
	}

	@Override
	public Comparator<?> getSort() {
		return this.sort;
	}

	@Override
	public int getOffset() {
		return this.offset;
	}

	@Override
	public int getRows() {
		return this.rows;
	}
}
