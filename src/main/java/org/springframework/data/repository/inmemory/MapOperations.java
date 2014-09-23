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

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * {@link InMemoryOperations} implementation using delegating to {@link MapAdapter}.
 * 
 * @author Christoph Strobl
 */
public class MapOperations extends AbstractInMemoryOperations {

	private final MapAdapter map = new MapAdapter();

	@Override
	public <T> List<T> read(Expression filter, final Class<T> type) {
		return filterMatchingRange(read(type), filter, -1, -1);
	}

	@Override
	public <T> List<T> read(int offset, int rows, final Class<T> type) {
		return filterMatchingRange(read(type), null, offset, rows);
	}

	@Override
	public <T> List<T> read(Expression filter, int offset, int rows, final Class<T> type) {
		return filterMatchingRange(read(type), filter, offset, rows);
	}

	@Override
	public long count(Expression filter, Class<?> type) {
		return filterMatchingRange(read(type), filter, -1, -1).size();
	}

	private <T> List<T> filterMatchingRange(Iterable<T> source, Expression filterExpression, int offset, int rows) {

		List<T> result = new ArrayList<T>();

		boolean compareOffsetAndRows = 0 <= offset || 0 <= rows;
		int remainingRows = rows;
		int curPos = 0;

		for (T candidate : source) {

			boolean matches = filterExpression == null;

			if (!matches) {
				try {
					matches = filterExpression.getValue(candidate, Boolean.class);
				} catch (SpelEvaluationException e) {
					((SpelExpression) filterExpression).getEvaluationContext().setVariable("it", candidate);
					matches = ((SpelExpression) filterExpression).getValue(Boolean.class);
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

	@Override
	protected InMemoryAdapter getAdapter() {
		return this.map;
	}

}
