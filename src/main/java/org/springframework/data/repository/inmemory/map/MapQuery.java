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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.inmemory.BasicInMemoryQuery;
import org.springframework.data.repository.inmemory.InMemoryQuery;
import org.springframework.expression.Expression;

/**
 * {@link InMemoryQuery} implementation to be used for executing {@link Expression} based queries.
 * 
 * @author Christoph Strobl
 */
public class MapQuery extends BasicInMemoryQuery {

	private final Expression criteria;

	public MapQuery(Expression criteria) {
		this.criteria = criteria;
	}

	public MapQuery skip(int offset) {
		setOffset(offset);
		return this;
	}

	public MapQuery limit(int rows) {
		setRows(rows);
		return this;
	}

	@Override
	public Expression getCritieria() {
		return this.criteria;
	}

	public MapQuery orderBy(Sort sort) {
		super.orderBy(sort);
		return this;
	}

}
