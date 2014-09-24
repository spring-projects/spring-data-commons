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

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public abstract class InMemoryPartTreeQuery<T, ID extends Serializable> implements RepositoryQuery {

	private final QueryMethod queryMethod;
	private final InMemoryOperations inMemoryOps;

	protected InMemoryPartTreeQuery(QueryMethod queryMethod, InMemoryOperations inMemoryOps) {
		this.queryMethod = queryMethod;
		this.inMemoryOps = inMemoryOps;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object execute(Object[] parameters) {

		// TODO: check usage of this.evaluationContextProvider at this point
		InMemoryQuery q = getQueryCreator(parameters).createQuery();

		if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {

			Pageable page = (Pageable) parameters[queryMethod.getParameters().getPageableIndex()];
			q.setOffset(page.getOffset());
			q.setRows(page.getPageSize());

			List<T> result = (List<T>) this.inMemoryOps.read(q, queryMethod.getEntityInformation().getJavaType());

			long count = queryMethod.isSliceQuery() ? 0 : inMemoryOps.count(q, queryMethod.getEntityInformation()
					.getJavaType());

			return new PageImpl<T>(result, page, count);
		}
		if (queryMethod.isCollectionQuery()) {

			return this.inMemoryOps.read(q, queryMethod.getEntityInformation().getJavaType());
		}
		if (queryMethod.isQueryForEntity()) {

			List<?> result = this.inMemoryOps.read(q, queryMethod.getEntityInformation().getJavaType());
			return CollectionUtils.isEmpty(result) ? null : result.get(0);
		}
		throw new UnsupportedOperationException();
	}

	public abstract AbstractQueryCreator<? extends InMemoryQuery, ?> getQueryCreator(Object[] parameters);
}
