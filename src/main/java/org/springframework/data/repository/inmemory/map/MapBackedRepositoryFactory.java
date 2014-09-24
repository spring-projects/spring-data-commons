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

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.inmemory.InMemoryPartTreeQuery;
import org.springframework.data.repository.inmemory.InMemoryQuery;
import org.springframework.data.repository.inmemory.InMemoryRepositoryFactory;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
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
		private MapOperations inMemoryOperations;

		public MapQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider,
				MapOperations inMemoryOperations) {
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

	public static class SpelQueryCreator extends AbstractQueryCreator<MapQuery, String> {

		private SpelExpression expression;

		public SpelQueryCreator(PartTree tree, ParameterAccessor parameters) {
			super(tree, parameters);
			this.expression = toPredicateExpression(tree);
		}

		@Override
		protected String create(Part part, Iterator<Object> iterator) {
			return "";
		}

		@Override
		protected String and(Part part, String base, Iterator<Object> iterator) {
			return "";
		}

		@Override
		protected String or(String base, String criteria) {
			return "";
		}

		@Override
		protected MapQuery complete(String criteria, Sort sort) {
			return new MapQuery(this.expression);
		}

		protected SpelExpression toPredicateExpression(PartTree tree) {

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
						default:
							throw new InvalidDataAccessApiUsageException(String.format("Found invalid part '%s' in query",
									part.getType()));
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

	}

	public static class ExpressionPartTreeQuery<T, ID extends Serializable> extends InMemoryPartTreeQuery<T, ID> {

		private EvaluationContextProvider evaluationContextProvider;

		public ExpressionPartTreeQuery(QueryMethod queryMethod, EvaluationContextProvider evalContextProvider,
				MapOperations inMemoryOperations) {
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
	protected MapOperations getInMemoryOperations() {
		return this.mapOps;
	}

}
