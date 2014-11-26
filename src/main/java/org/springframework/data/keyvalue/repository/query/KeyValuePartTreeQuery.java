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
package org.springframework.data.keyvalue.repository.query;

import java.lang.reflect.Constructor;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link RepositoryQuery} implementation deriving queries from {@link PartTree} using a predefined
 * {@link AbstractQueryCreator}.
 * 
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.10
 */
public class KeyValuePartTreeQuery implements RepositoryQuery {

	private final EvaluationContextProvider evaluationContextProvider;
	private final QueryMethod queryMethod;
	private final KeyValueOperations keyValueOperations;
	private final Class<? extends AbstractQueryCreator<?, ?>> queryCreator;

	private KeyValueQuery<?> query;

	public KeyValuePartTreeQuery(QueryMethod queryMethod, EvaluationContextProvider evalContextProvider,
			KeyValueOperations keyValueOperations, Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {

		this.queryMethod = queryMethod;
		this.keyValueOperations = keyValueOperations;
		this.evaluationContextProvider = evalContextProvider;
		this.queryCreator = queryCreator;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object execute(Object[] parameters) {

		KeyValueQuery<?> query = prepareQuery(parameters);

		if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {

			Pageable page = (Pageable) parameters[queryMethod.getParameters().getPageableIndex()];
			query.setOffset(page.getOffset());
			query.setRows(page.getPageSize());

			List<?> result = this.keyValueOperations.find(query, queryMethod.getEntityInformation().getJavaType());

			long count = queryMethod.isSliceQuery() ? 0 : keyValueOperations.count(query, queryMethod.getEntityInformation()
					.getJavaType());

			return new PageImpl(result, page, count);

		} else if (queryMethod.isCollectionQuery()) {

			return this.keyValueOperations.find(query, queryMethod.getEntityInformation().getJavaType());

		} else if (queryMethod.isQueryForEntity()) {

			List<?> result = this.keyValueOperations.find(query, queryMethod.getEntityInformation().getJavaType());
			return CollectionUtils.isEmpty(result) ? null : result.get(0);

		}

		throw new UnsupportedOperationException("Query method not supported.");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private KeyValueQuery<?> prepareQuery(Object[] parameters) {

		ParametersParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(), parameters);

		if (this.query == null) {
			this.query = createQuery(accessor);
		}

		KeyValueQuery<?> q = new KeyValueQuery(this.query.getCritieria());

		if (accessor.getPageable() != null) {
			q.setOffset(accessor.getPageable().getOffset());
			q.setRows(accessor.getPageable().getPageSize());
		} else {
			q.setOffset(-1);
			q.setRows(-1);
		}

		if (accessor.getSort() != null) {
			q.setSort(accessor.getSort());
		} else {
			q.setSort(this.query.getSort());
		}

		if (q.getCritieria() instanceof SpelExpression) {
			EvaluationContext context = this.evaluationContextProvider.getEvaluationContext(getQueryMethod().getParameters(),
					parameters);
			((SpelExpression) q.getCritieria()).setEvaluationContext(context);
		}

		return q;
	}

	public KeyValueQuery<?> createQuery(ParametersParameterAccessor accessor) {

		PartTree tree = new PartTree(getQueryMethod().getName(), getQueryMethod().getEntityInformation().getJavaType());

		Constructor<? extends AbstractQueryCreator<?, ?>> constructor = (Constructor<? extends AbstractQueryCreator<?, ?>>) ClassUtils
				.getConstructorIfAvailable(queryCreator, PartTree.class, ParameterAccessor.class);
		return (KeyValueQuery<?>) BeanUtils.instantiateClass(constructor, tree, accessor).createQuery();
	}
}
