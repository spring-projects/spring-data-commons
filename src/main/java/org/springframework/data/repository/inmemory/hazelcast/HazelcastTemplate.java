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
package org.springframework.data.repository.inmemory.hazelcast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.inmemory.AbstractInMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.data.repository.inmemory.InMemoryCallback;

/**
 * @author Christoph Strobl
 */
public class HazelcastTemplate extends AbstractInMemoryOperations<HazelcastQuery> {

	private HazelcastAdapter adapter;

	public HazelcastTemplate() {
		this(new HazelcastAdapter());
	}

	public HazelcastTemplate(HazelcastAdapter adapter) {
		super();
		this.adapter = adapter;
	}

	@Override
	public <T> List<T> read(final int offset, final int rows, final Class<T> type) {

		return execute(new InMemoryCallback<List<T>>() {

			@Override
			public List<T> doInMemory(InMemoryAdapter adapter) {

				List<T> tmp = new ArrayList<T>(adapter.getAllOf(type));

				if (offset > tmp.size()) {
					return Collections.emptyList();
				}
				int rowsToUse = (offset + rows > tmp.size()) ? tmp.size() - offset : rows;
				return new ArrayList<T>(adapter.getAllOf(type)).subList(offset, offset + rowsToUse);
			}
		});
	}

	@Override
	public <T> List<T> read(Sort sort, Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> List<T> read(int offset, int rows, Sort sort, Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void destroy() throws Exception {
		this.adapter.destroy();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> List<T> doRead(HazelcastQuery query, Class<T> type) {

		Collection<T> results = adapter.getMap(type).values(query.getCritieria());

		if (query.getRows() > 0 && query.getOffset() >= 0) {
			int rowsToUse = (query.getOffset() + query.getRows() > results.size()) ? results.size() - query.getOffset()
					: query.getRows();
			return new ArrayList<T>(adapter.getAllOf(type)).subList(query.getOffset(), query.getOffset() + rowsToUse);
		}
		return new ArrayList<T>(results);
	}

	@Override
	protected long doCount(HazelcastQuery query, Class<?> type) {
		return doRead(query, type).size();
	}

	@Override
	protected InMemoryAdapter getAdapter() {
		return this.adapter;
	}

}
