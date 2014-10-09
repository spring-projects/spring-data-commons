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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.NullHandling;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.inmemory.AbstractInMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.data.repository.inmemory.InMemoryOperations;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.util.comparator.CompoundComparator;

/**
 * {@link InMemoryOperations} implementation using delegating to {@link MapAdapter}.
 * 
 * @author Christoph Strobl
 */
public class MapTemplate extends AbstractInMemoryOperations<MapQuery> {

	private final MapAdapter map = new MapAdapter();

	public MapTemplate() {
		super();
	}

	@SuppressWarnings("rawtypes")
	public MapTemplate(
			MappingContext<? extends PersistentEntity<?, ? extends PersistentProperty>, ? extends PersistentProperty<?>> mappingContext) {
		super(mappingContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.AbstractInMemoryOperations#doRead(org.springframework.data.repository.inmemory.BasicInMemoryQuery, java.lang.Class)
	 */
	@Override
	protected <T> List<T> doRead(MapQuery query, final Class<T> type) {
		return sortAndFilterMatchingRange(read(type), query);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.AbstractInMemoryOperations#doCount(org.springframework.data.repository.inmemory.BasicInMemoryQuery, java.lang.Class)
	 */
	@Override
	protected long doCount(MapQuery query, Class<?> type) {
		return doRead(new MapQuery(query.getCritieria()), type).size();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(int, int, java.lang.Class)
	 */
	@Override
	public <T> List<T> read(int offset, int rows, final Class<T> type) {
		return read(new MapQuery(null).skip(offset).limit(rows), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(org.springframework.data.domain.Sort, java.lang.Class)
	 */
	@Override
	public <T> List<T> read(Sort sort, Class<T> type) {
		return read(new MapQuery(null).orderBy(sort), type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryOperations#read(int, int, org.springframework.data.domain.Sort, java.lang.Class)
	 */
	@Override
	public <T> List<T> read(int offset, int rows, Sort sort, Class<T> type) {
		return read(new MapQuery(null).skip(offset).limit(rows).orderBy(sort), type);
	}

	@SuppressWarnings({ "unchecked" })
	private <T> List<T> sortAndFilterMatchingRange(List<T> source, MapQuery query) {

		if (query.getSort() != null) {
			Collections.sort(source, createSort(query.getSort()));
		}

		return filterMatchingRange(source, query);
	}

	private <T> List<T> filterMatchingRange(Iterable<T> source, MapQuery query) {

		List<T> result = new ArrayList<T>();

		boolean compareOffsetAndRows = 0 < query.getOffset() || 0 <= query.getRows();
		int remainingRows = query.getRows();
		int curPos = 0;

		for (T candidate : source) {

			boolean matches = query.getCritieria() == null;

			if (!matches) {
				try {
					matches = query.getCritieria().getValue(candidate, Boolean.class);
				} catch (SpelEvaluationException e) {
					((SpelExpression) query.getCritieria()).getEvaluationContext().setVariable("it", candidate);
					matches = ((SpelExpression) query.getCritieria()).getValue(Boolean.class);
				}
			}

			if (matches) {
				if (compareOffsetAndRows) {
					if (curPos >= query.getOffset() && query.getRows() > 0) {
						result.add(candidate);
						remainingRows--;
						if (remainingRows <= 0) {
							break;
						}
					}
					curPos++;
				} else {
					result.add(candidate);
				}
			}
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Comparator createSort(Sort sort) {

		CompoundComparator compoundComperator = new CompoundComparator();
		for (Order order : sort) {

			SpelSort<?> spelSort = new SpelSort(order.getProperty());

			if (Direction.DESC.equals(order.getDirection())) {

				spelSort.desc();

				if (order.getNullHandling() != null && !NullHandling.NATIVE.equals(order.getNullHandling())) {
					spelSort = NullHandling.NULLS_FIRST.equals(order.getNullHandling()) ? spelSort.nullsFirst() : spelSort
							.nullsLast();
				}
			}
			compoundComperator.addComparator(spelSort);
		}

		return compoundComperator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.AbstractInMemoryOperations#getAdapter()
	 */
	@Override
	protected InMemoryAdapter getAdapter() {
		return this.map;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		this.map.clear();
	}

}
