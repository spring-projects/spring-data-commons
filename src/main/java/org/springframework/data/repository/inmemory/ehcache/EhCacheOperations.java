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

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.inmemory.AbstractInMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.data.repository.inmemory.InMemoryCallback;

/**
 * @author Christoph Strobl
 */
public class EhCacheOperations extends AbstractInMemoryOperations<EhCacheQuery> {

	private EhCacheAdapter adapter;

	public EhCacheOperations() {
		this(new EhCacheAdapter());
	}

	public EhCacheOperations(EhCacheAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public <T> List<T> read(final int offset, final int rows, final Class<T> type) {

		return execute(new InMemoryCallback<List<T>>() {

			@Override
			public List<T> doInMemory(InMemoryAdapter adapter) {
				return new ArrayList<T>(adapter.getAllOf(type)).subList(offset, offset + rows);
			}
		});
	}

	@Override
	protected EhCacheAdapter getAdapter() {
		return this.adapter;
	}

	@Override
	protected <T> List<T> doRead(EhCacheQuery query, Class<T> type) {

		Query q = prepareQuery(query, type);
		Results result = q.execute();

		ResultConverter<T> conv = new ResultConverter<T>();
		ListConverter<Result, T> listConc = new ListConverter<Result, T>(conv);

		if (query.getRows() > 0 && query.getOffset() >= 0) {
			return listConc.convert(result.range(query.getOffset(), query.getRows()));
		}
		return listConc.convert(result.all());
	}

	private class ResultConverter<T> implements Converter<Result, T> {

		@SuppressWarnings("unchecked")
		@Override
		public T convert(Result source) {
			return (T) source.getValue();
		}
	}

	private class ListConverter<S, T> implements Converter<List<S>, List<T>> {

		private Converter<S, T> itemConverter;

		/**
		 * @param itemConverter The {@link Converter} to use for converting individual List items
		 */
		public ListConverter(Converter<S, T> itemConverter) {
			this.itemConverter = itemConverter;
		}

		public List<T> convert(List<S> source) {
			if (source == null) {
				return null;
			}
			List<T> results = new ArrayList<T>();
			for (S result : source) {
				results.add(itemConverter.convert(result));
			}
			return results;
		}

	}

	@Override
	protected long doCount(EhCacheQuery query, Class<?> type) {

		Query q = prepareQuery(query, type);
		q.end();
		return q.execute().size();
	}

	@Override
	public void destroy() throws Exception {
		getAdapter().clear();
	}

	private <T> Query prepareQuery(EhCacheQuery query, Class<T> type) {

		Query cacheQuery = getAdapter().getCache(type).createQuery().includeValues();
		cacheQuery.addCriteria(query.getCritieria());
		cacheQuery.end();
		return cacheQuery;
	}

}
