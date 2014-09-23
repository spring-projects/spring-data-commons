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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.inmemory.InMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryRepositoryFactory;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

public class MapBackedRepositoryFactory<T, ID extends Serializable> extends InMemoryRepositoryFactory<T, ID> {

	private final MapOperations mapOps;

	public MapBackedRepositoryFactory(MapOperations mapOperations) {
		this.mapOps = mapOperations;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new MapQueryLookupStrategy<T, ID>(key, evaluationContextProvider, getInMemoryOperations());
	}

	static class MapQueryLookupStrategy<T, ID extends Serializable> implements QueryLookupStrategy {

		private EvaluationContextProvider evaluationContextProvider;
		private InMemoryOperations inMemoryOperations;

		public MapQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider,
				InMemoryOperations inMemoryOperations) {
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

	public static class ExpressionPartTreeQuery<T, ID extends Serializable> implements RepositoryQuery {

		private QueryMethod queryMethod;
		private SpelExpression expression;
		private EvaluationContextProvider evaluationContextProvider;
		private InMemoryOperations inMemoryOperations;

		public ExpressionPartTreeQuery(QueryMethod queryMethod, EvaluationContextProvider evalContextProvider,
				InMemoryOperations inMemoryOperations) {
			this.queryMethod = queryMethod;
			this.evaluationContextProvider = evalContextProvider;
			this.expression = toPredicateExpression(queryMethod);
			this.inMemoryOperations = inMemoryOperations;
		}

		protected SpelExpression toPredicateExpression(QueryMethod queryMethod) {

			PartTree tree = new PartTree(queryMethod.getName(), queryMethod.getEntityInformation().getJavaType());

			int parameterIndex = 0;
			StringBuilder sb = new StringBuilder();

			for (Iterator<OrPart> orPartIter = tree.iterator(); orPartIter.hasNext();) {

				OrPart orPart = orPartIter.next();

				int partCnt = 0;
				StringBuilder partBuilder = new StringBuilder();
				for (Iterator<Part> partIter = orPart.iterator(); partIter.hasNext();) {

					Part part = partIter.next();
					partBuilder.append("#it?.");
					partBuilder.append(part.getProperty().toDotPath().replace(".", ".?"));

					switch (part.getType()) {
						case TRUE:
							partBuilder.append("?.equals(true)");
							break;
						case FALSE:
							partBuilder.append("?.equals(false)");
							break;
						case SIMPLE_PROPERTY:
							partBuilder.append("?.equals(").append("[").append(parameterIndex++).append("])");
							break;
						case IS_NULL:
							partBuilder.append(" == null");
							break;
						case STARTING_WITH:
						case LIKE:
							partBuilder.append("?.startsWith(").append("[").append(parameterIndex++).append("])");
							break;
						case GREATER_THAN:
							partBuilder.append(">").append("[").append(parameterIndex++).append("])");
							break;
						case GREATER_THAN_EQUAL:
							partBuilder.append(">=").append("[").append(parameterIndex++).append("])");
							break;
					}

					if (partIter.hasNext()) {
						partBuilder.append("&&");
					}

					partCnt++;
				}

				if (partCnt > 1) {
					sb.append("(").append(partBuilder).append(")");
				} else {
					sb.append(partBuilder);
				}

				if (orPartIter.hasNext()) {
					sb.append("||");
				}

			}

			return (SpelExpression) new SpelExpressionParser().parseExpression(sb.toString());
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object execute(Object[] parameters) {

			expression.setEvaluationContext(new StandardEvaluationContext(parameters));
			MapQuery q = new MapQuery(expression);

			if (queryMethod.isPageQuery() || queryMethod.isSliceQuery()) {

				Pageable page = (Pageable) parameters[queryMethod.getParameters().getPageableIndex()];
				q.skip(page.getOffset()).limit(page.getPageSize());

				List<T> result = (List<T>) this.inMemoryOperations.read(q, queryMethod.getEntityInformation().getJavaType());

				long count = queryMethod.isSliceQuery() ? 0 : inMemoryOperations.count(q, queryMethod.getEntityInformation()
						.getJavaType());

				return new PageImpl<T>(result, page, count);
			}
			if (queryMethod.isCollectionQuery()) {

				return this.inMemoryOperations.read(new MapQuery(expression), queryMethod.getEntityInformation().getJavaType());
			}
			if (queryMethod.isQueryForEntity()) {

				List<?> result = this.inMemoryOperations.read(new MapQuery(expression), queryMethod.getEntityInformation()
						.getJavaType());
				return CollectionUtils.isEmpty(result) ? null : result.get(0);
			}
			throw new UnsupportedOperationException();
		}

		@Override
		public QueryMethod getQueryMethod() {
			return queryMethod;
		}
	}

	@Override
	protected MapOperations getInMemoryOperations() {
		return this.mapOps;
	}

}
