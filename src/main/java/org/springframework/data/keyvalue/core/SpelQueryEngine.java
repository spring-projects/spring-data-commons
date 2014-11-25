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
package org.springframework.data.keyvalue.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * {@link QueryEngine} implementation specific for executing {@link SpelExpression} based {@link KeyValueQuery} against
 * {@link KeyValueAdapter}.
 * 
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 */
public class SpelQueryEngine<T extends KeyValueAdapter> extends
		QueryEngine<KeyValueAdapter, SpelExpression, Comparator<?>> {

	public SpelQueryEngine() {
		super(SpelCriteriaAccessor.INSTANCE, SpelSortAccessor.INSTNANCE);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.QueryEngine#execute(java.lang.Object, java.lang.Object, int, int, java.io.Serializable)
	 */
	@Override
	public Collection<?> execute(SpelExpression criteria, Comparator<?> sort, int offset, int rows, Serializable keyspace) {
		return sortAndFilterMatchingRange(getAdapter().getAllOf(keyspace), criteria, sort, offset, rows);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.keyvalue.core.QueryEngine#count(java.lang.Object, java.io.Serializable)
	 */
	@Override
	public long count(SpelExpression criteria, Serializable keyspace) {
		return filterMatchingRange(getAdapter().getAllOf(keyspace), criteria, -1, -1).size();
	}

	@SuppressWarnings({ "unchecked" })
	private List<?> sortAndFilterMatchingRange(Collection<?> source, SpelExpression criteria, Comparator sort,
			int offset, int rows) {

		List<?> tmp = new ArrayList(source);
		if (sort != null) {
			Collections.sort(tmp, sort);
		}

		return filterMatchingRange(tmp, criteria, offset, rows);
	}

	private <S> List<S> filterMatchingRange(Iterable<S> source, SpelExpression criteria, int offset, int rows) {

		List<S> result = new ArrayList<S>();

		boolean compareOffsetAndRows = 0 < offset || 0 <= rows;
		int remainingRows = rows;
		int curPos = 0;

		for (S candidate : source) {

			boolean matches = criteria == null;

			if (!matches) {
				try {
					matches = criteria.getValue(candidate, Boolean.class);
				} catch (SpelEvaluationException e) {
					criteria.getEvaluationContext().setVariable("it", candidate);
					matches = criteria.getValue(Boolean.class);
				}
			}

			if (matches) {
				if (compareOffsetAndRows) {
					if (curPos >= offset && rows > 0) {
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

}
