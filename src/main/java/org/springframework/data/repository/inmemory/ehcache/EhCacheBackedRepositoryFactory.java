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
package org.springframework.data.repository.inmemory.ehcache;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;

import net.sf.ehcache.search.expression.Criteria;
import net.sf.ehcache.search.expression.EqualTo;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.inmemory.InMemoryRepositoryFactory;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public class EhCacheBackedRepositoryFactory<T, ID extends Serializable> extends InMemoryRepositoryFactory<T, ID> {

	private EhCacheOperations cacheOps;

	public EhCacheBackedRepositoryFactory(EhCacheOperations cacheOperations) {
		this.cacheOps = cacheOperations;
	}

	@Override
	protected EhCacheOperations getInMemoryOperations() {
		return cacheOps;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new EhCacheQueryLookupStrategy<T, Serializable>(key, getInMemoryOperations());
	}

	static class EhCacheQueryLookupStrategy<T, ID extends Serializable> implements QueryLookupStrategy {

		private EhCacheOperations inMemoryOperations;

		public EhCacheQueryLookupStrategy(Key key, EhCacheOperations inMemoryOperations) {
			this.inMemoryOperations = inMemoryOperations;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			QueryMethod queryMethod = new QueryMethod(method, metadata);
			return new EhCachePartTreeQuery<T, ID>(queryMethod, this.inMemoryOperations);
		}

	}

	public static class EhCachePartTreeQuery<T, ID extends Serializable> implements RepositoryQuery {

		private QueryMethod queryMethod;
		private EhCacheOperations inMemoryOperations;

		public EhCachePartTreeQuery(QueryMethod queryMethod, EhCacheOperations inMemoryOperations) {
			super();
			this.queryMethod = queryMethod;
			this.inMemoryOperations = inMemoryOperations;
		}

		protected Criteria toEhCacheCriteria(QueryMethod queryMethod) {

			PartTree tree = new PartTree(queryMethod.getName(), queryMethod.getEntityInformation().getJavaType());

			int parameterIndex = 0;
			StringBuilder sb = new StringBuilder();

			Criteria criteria = null;

			for (Iterator<OrPart> orPartIter = tree.iterator(); orPartIter.hasNext();) {

				OrPart orPart = orPartIter.next();

				int partCnt = 0;
				StringBuilder partBuilder = new StringBuilder();
				for (Iterator<Part> partIter = orPart.iterator(); partIter.hasNext();) {

					Part part = partIter.next();

					Criteria c = null;
					switch (part.getType()) {
						case TRUE:
							c = new EqualTo(part.getProperty().toDotPath(), true);
							break;
						case FALSE:
							c = new EqualTo(part.getProperty().toDotPath(), true);
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
						if (criteria == null) {
							criteria = c;
						} else {
							criteria.and(c);
						}
					}

					partCnt++;
				}

				if (partCnt > 1) {
					sb.append("(").append(partBuilder).append(")");
				} else {
					sb.append(partBuilder);
				}

				if (orPartIter.hasNext()) {
					if (criteria == null) {
						// criteria.or(other)
					} else {
						// TODO:
					}
				}

			}

			return null;
		}

		@Override
		public Object execute(Object[] parameters) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public QueryMethod getQueryMethod() {
			return queryMethod;
		}

	}
}
