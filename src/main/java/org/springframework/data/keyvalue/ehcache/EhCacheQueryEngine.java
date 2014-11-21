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
package org.springframework.data.keyvalue.ehcache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.expression.Criteria;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.CriteriaAccessor;
import org.springframework.data.keyvalue.core.QueryEngine;
import org.springframework.data.keyvalue.core.SortAccessor;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 */
public class EhCacheQueryEngine extends QueryEngine<EhCacheKeyValueAdapter, Criteria, Map<Attribute<?>, Direction>> {

	public EhCacheQueryEngine() {
		super(EhCacheCriteriaAccessor.INSTANCE, EhCacheSortAccessor.INSTNANCE);
	}

	@Override
	public Collection<?> execute(Criteria criteria, Map<Attribute<?>, Direction> sort, int offset, int rows,
			Serializable keyspace) {

		Query cacheQuery = prepareQuery(criteria, sort, keyspace);
		if (cacheQuery == null) {
			return Collections.emptyList();
		}

		Results result = cacheQuery.execute();

		ListConverter<Result, Object> listConc = new ListConverter<Result, Object>(ResultConverter.INSTANCE);

		if (rows > 0 && offset >= 0) {
			return listConc.convert(result.range(offset, rows));
		}
		return listConc.convert(result.all());
	}

	@Override
	public long count(Criteria criteria, Serializable keyspace) {

		Query q = prepareQuery(criteria, null, keyspace);
		if (q == null) {
			return 0;
		}

		return q.execute().size();
	}

	private Query prepareQuery(Criteria criteria, Map<Attribute<?>, Direction> sort, Serializable keyspace) {

		Cache cache = getAdapter().getCache(keyspace);
		if (cache == null) {
			return null;
		}

		Query cacheQuery = cache.createQuery().includeValues();

		if (criteria != null) {
			cacheQuery.addCriteria(criteria);
		}

		if (!CollectionUtils.isEmpty(sort)) {
			for (Map.Entry<Attribute<?>, Direction> order : sort.entrySet()) {
				cacheQuery.addOrderBy(order.getKey(), order.getValue());
			}
		}
		cacheQuery.end();
		return cacheQuery;
	}

	static enum EhCacheCriteriaAccessor implements CriteriaAccessor<Criteria> {
		INSTANCE;

		@Override
		public Criteria resolve(KeyValueQuery<?> query) {

			if (query == null || query.getCritieria() == null) {
				return null;
			}

			if (query.getCritieria() instanceof Criteria) {
				return (Criteria) query.getCritieria();
			}

			throw new UnsupportedOperationException();
		}

	}

	static enum EhCacheSortAccessor implements SortAccessor<Map<Attribute<?>, Direction>> {

		INSTNANCE;

		@SuppressWarnings({ "rawtypes" })
		@Override
		public Map<Attribute<?>, Direction> resolve(KeyValueQuery<?> query) {

			if (query == null || query.getSort() == null) {
				return null;
			}

			Map<Attribute<?>, Direction> attributes = new LinkedHashMap<Attribute<?>, Direction>();

			for (Sort.Order order : query.getSort()) {
				attributes.put(new Attribute(order.getProperty()), org.springframework.data.domain.Sort.Direction.ASC
						.equals(order.getDirection()) ? net.sf.ehcache.search.Direction.ASCENDING
						: net.sf.ehcache.search.Direction.DESCENDING);
			}

			return attributes;
		}
	}

	static enum ResultConverter implements Converter<Result, Object> {
		INSTANCE;

		@Override
		public Object convert(Result source) {
			return source.getValue();
		}
	}

}
