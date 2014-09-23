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
package org.springframework.data.repository.inmemory.ehcache;

import net.sf.ehcache.search.expression.Criteria;

import org.springframework.data.repository.inmemory.InMemoryQuery;

/**
 * @author Christoph Strobl
 */
public class EhCacheQuery implements InMemoryQuery {

	private Criteria criteria;
	private int offset = -1;
	private int rows = -1;

	public EhCacheQuery(Criteria criteria) {
		this.criteria = criteria;
	}

	@Override
	public Criteria getCritieria() {
		return this.criteria;
	}

	@Override
	public Object getSort() {
		return null;
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
