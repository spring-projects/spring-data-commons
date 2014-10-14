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
package org.springframework.data.keyvalue.repository.support;

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.query.KeyValueQuery;
import org.springframework.data.keyvalue.repository.BasicKeyValueRepository;
import org.springframework.data.keyvalue.repository.QueryDslKeyValueRepository;
import org.springframework.data.keyvalue.repository.query.SpelQueryCreator;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 * @param <ID>
 */
public class KeyValueRepositoryFactory extends RepositoryFactorySupport {

	private final KeyValueOperations keyValueOperations;
	private final MappingContext<?, ?> context;

	public KeyValueRepositoryFactory(KeyValueOperations keyValueOperations) {

		Assert.notNull(keyValueOperations, "KeyValueOperations must not be 'null' when creating factory.");
		this.keyValueOperations = keyValueOperations;
		this.context = keyValueOperations.getMappingContext();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		PersistentEntity<T, ?> entity = (PersistentEntity<T, ?>) context.getPersistentEntity(domainClass);
		PersistentEntityInformation<T, ID> entityInformation = new PersistentEntityInformation<T, ID>(entity);
		return entityInformation;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Object getTargetRepository(RepositoryMetadata metadata) {

		EntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());
		if (ClassUtils.isAssignable(QueryDslPredicateExecutor.class, metadata.getRepositoryInterface())) {
			return new QueryDslKeyValueRepository(entityInformation, keyValueOperations);
		}
		return new BasicKeyValueRepository(entityInformation, keyValueOperations);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return isQueryDslRepository(metadata.getRepositoryInterface()) ? QueryDslKeyValueRepository.class
				: BasicKeyValueRepository.class;
	}

	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 * 
	 * @param repositoryInterface
	 * @return
	 */
	private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
		return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new KeyValueQueryLookupStrategy(key, evaluationContextProvider, this.keyValueOperations);
	}

	static class KeyValueQueryLookupStrategy implements QueryLookupStrategy {

		private EvaluationContextProvider evaluationContextProvider;
		private KeyValueOperations keyValueOperations;

		public KeyValueQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider,
				KeyValueOperations keyValueOperations) {
			this.evaluationContextProvider = evaluationContextProvider;
			this.keyValueOperations = keyValueOperations;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			QueryMethod queryMethod = new QueryMethod(method, metadata);
			return new ExpressionPartTreeQuery(queryMethod, evaluationContextProvider, this.keyValueOperations);
		}

	}

	public static class ExpressionPartTreeQuery implements RepositoryQuery {

		private EvaluationContextProvider evaluationContextProvider;
		private final QueryMethod queryMethod;
		private final KeyValueOperations keyValueOperations;

		public ExpressionPartTreeQuery(QueryMethod queryMethod, EvaluationContextProvider evalContextProvider,
				KeyValueOperations keyValueOperations) {

			this.queryMethod = queryMethod;
			this.keyValueOperations = keyValueOperations;
			this.evaluationContextProvider = evalContextProvider;
		}

		@Override
		public QueryMethod getQueryMethod() {
			return queryMethod;
		}

		public KeyValueQuery<SpelExpression> getQuery(Object[] parameters) {

			ParametersParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(),
					parameters);
			PartTree tree = new PartTree(getQueryMethod().getName(), getQueryMethod().getEntityInformation().getJavaType());

			// TODO: check usage of evaluationContextProvider within the query creator
			KeyValueQuery<SpelExpression> query = new SpelQueryCreator(tree, accessor).createQuery();
			query.getCritieria().setEvaluationContext(new StandardEvaluationContext(parameters));
			return query;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Object execute(Object[] parameters) {

			KeyValueQuery<SpelExpression> q = getQuery(parameters);

			if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {

				Pageable page = (Pageable) parameters[queryMethod.getParameters().getPageableIndex()];
				q.setOffset(page.getOffset());
				q.setRows(page.getPageSize());

				List<?> result = this.keyValueOperations.find(q, queryMethod.getEntityInformation().getJavaType());

				long count = queryMethod.isSliceQuery() ? 0 : keyValueOperations.count(q, queryMethod.getEntityInformation()
						.getJavaType());

				return new PageImpl(result, page, count);
			}
			if (queryMethod.isCollectionQuery()) {

				return this.keyValueOperations.find(q, queryMethod.getEntityInformation().getJavaType());
			}
			if (queryMethod.isQueryForEntity()) {

				List<?> result = this.keyValueOperations.find(q, queryMethod.getEntityInformation().getJavaType());
				return CollectionUtils.isEmpty(result) ? null : result.get(0);
			}
			throw new UnsupportedOperationException("Query method not supported.");
		}

	}
}
