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

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.inmemory.InMemoryPartTreeQuery;
import org.springframework.data.repository.inmemory.InMemoryQuery;
import org.springframework.data.repository.inmemory.InMemoryRepositoryFactory;
import org.springframework.data.repository.inmemory.QueryDslInMemoryRepository;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public class MapBackedRepositoryFactory<T, ID extends Serializable> extends InMemoryRepositoryFactory<T, ID> {

	private final MapTemplate mapOps;

	public MapBackedRepositoryFactory(MapTemplate mapOperations) {
		this.mapOps = mapOperations;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new MapQueryLookupStrategy<T, ID>(key, evaluationContextProvider, getInMemoryOperations());
	}

	static class MapQueryLookupStrategy<T, ID extends Serializable> implements QueryLookupStrategy {

		private EvaluationContextProvider evaluationContextProvider;
		private MapTemplate inMemoryOperations;

		public MapQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider,
				MapTemplate inMemoryOperations) {
			this.evaluationContextProvider = evaluationContextProvider;
			this.inMemoryOperations = inMemoryOperations;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			QueryMethod queryMethod = new QueryMethod(method, metadata);
			return new ExpressionPartTreeQuery<T, ID>(queryMethod, evaluationContextProvider, this.inMemoryOperations);
		}

	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {

		if (isQueryDslExecutor(metadata.getRepositoryInterface())) {
			return QueryDslInMemoryRepository.class;
		}
		return super.getRepositoryBaseClass(metadata);
	}

	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 * 
	 * @param repositoryInterface
	 * @return
	 */
	private boolean isQueryDslExecutor(Class<?> repositoryInterface) {

		return QUERY_DSL_PRESENT && ClassUtils.isAssignable(QueryDslPredicateExecutor.class, repositoryInterface);
	}

	public static class ExpressionPartTreeQuery<T, ID extends Serializable> extends InMemoryPartTreeQuery<T, ID> {

		private EvaluationContextProvider evaluationContextProvider;

		public ExpressionPartTreeQuery(QueryMethod queryMethod, EvaluationContextProvider evalContextProvider,
				MapTemplate inMemoryOperations) {
			super(queryMethod, inMemoryOperations);
			this.evaluationContextProvider = evalContextProvider;
		}

		@Override
		public InMemoryQuery getQuery(Object[] parameters) {

			ParametersParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(),
					parameters);
			PartTree tree = new PartTree(getQueryMethod().getName(), getQueryMethod().getEntityInformation().getJavaType());

			// TODO: check usage of evaluationContextProvider within the query creator
			MapQuery query = new SpelQueryCreator(tree, accessor).createQuery();
			((SpelExpression) query.getCritieria()).setEvaluationContext(new StandardEvaluationContext(parameters));
			return query;
		}

		@Override
		public Object execute(Object[] parameters) {

			return super.execute(parameters);
		}

	}

	@Override
	protected MapTemplate getInMemoryOperations() {
		return this.mapOps;
	}

}
