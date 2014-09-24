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
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.ILike;

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

	public static class EhCacheQueryCreator extends AbstractQueryCreator<EhCacheQuery, Criteria> {

		public EhCacheQueryCreator(PartTree tree, ParameterAccessor parameters) {
			super(tree, parameters);
		}

		public EhCacheQueryCreator(PartTree tree) {
			super(tree);
		}

		@Override
		protected Criteria create(Part part, Iterator<Object> iterator) {
			return from(part, iterator);
		}

		@Override
		protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {

			if (base == null) {
				return create(part, iterator);
			}
			return base.and(from(part, iterator));
		}

		@Override
		protected Criteria or(Criteria base, Criteria criteria) {
			return base.or(criteria);
		}

		@Override
		protected EhCacheQuery complete(Criteria criteria, Sort sort) {
			return new EhCacheQuery(criteria);
		}

		private Criteria from(Part part, Iterator<Object> iterator) {

			// TODO: complete list of supported types
			switch (part.getType()) {
				case TRUE:
					return new EqualTo(part.getProperty().toDotPath(), true);
				case FALSE:
					return new EqualTo(part.getProperty().toDotPath(), true);
				case SIMPLE_PROPERTY:
					return new EqualTo(part.getProperty().toDotPath(), iterator.next());
				case IS_NULL:
					return new EqualTo(part.getProperty().toDotPath(), null);
				case STARTING_WITH:
				case LIKE:
					return new ILike(part.getProperty().toDotPath(), iterator.next() + "*");
				case GREATER_THAN:
					return new GreaterThan(part.getProperty().toDotPath(), iterator.next());

				default:
					throw new InvalidDataAccessApiUsageException(
							String.format("Found invalid part '%s' in query", part.getType()));
			}
		}
	}

	public static class EhCachePartTreeQuery<T, ID extends Serializable> extends InMemoryPartTreeQuery<T, ID> {

		public EhCachePartTreeQuery(QueryMethod queryMethod, EhCacheOperations inMemoryOperations) {
			super(queryMethod, inMemoryOperations);
		}

		@Override
		public InMemoryQuery getQuery(Object[] parameters) {
			ParametersParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(),
					parameters);
			PartTree tree = new PartTree(getQueryMethod().getName(), getQueryMethod().getEntityInformation().getJavaType());
			return new EhCacheQueryCreator(tree, accessor).createQuery();
		}

	}
}
