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
package org.springframework.data.repository.inmemory.hazelcast;

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

import com.hazelcast.query.EntryObject;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;

/**
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public class HazelcastBackedRepositoryFactory<T, ID extends Serializable> extends InMemoryRepositoryFactory<T, ID> {

	private HazelcastTemplate hazelcastTemplate;

	public HazelcastBackedRepositoryFactory(HazelcastTemplate hazelcastTemplate) {
		this.hazelcastTemplate = hazelcastTemplate;
	}

	@Override
	protected HazelcastTemplate getInMemoryOperations() {
		return hazelcastTemplate;
	}

	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new HazelcastQueryLookupStrategy<T, Serializable>(key, getInMemoryOperations());
	}

	static class HazelcastQueryLookupStrategy<T, ID extends Serializable> implements QueryLookupStrategy {

		private HazelcastTemplate inMemoryOperations;

		public HazelcastQueryLookupStrategy(Key key, HazelcastTemplate inMemoryOperations) {
			this.inMemoryOperations = inMemoryOperations;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			QueryMethod queryMethod = new QueryMethod(method, metadata);
			return new HazelcastPartTreeQuery<T, ID>(queryMethod, this.inMemoryOperations);
		}

	}

	public static class HazelcastQueryCreator extends AbstractQueryCreator<HazelcastQuery, Predicate<?, ?>> {

		private PredicateBuilder predicateBuilder;

		public HazelcastQueryCreator(PartTree tree) {
			super(tree);
			this.predicateBuilder = new PredicateBuilder();
		}

		public HazelcastQueryCreator(PartTree tree, ParameterAccessor parameters) {
			super(tree, parameters);
			this.predicateBuilder = new PredicateBuilder();
		}

		@Override
		protected Predicate<?, ?> create(Part part, Iterator<Object> iterator) {
			return from(predicateBuilder, part, iterator);
		}

		@Override
		protected Predicate<?, ?> and(Part part, Predicate<?, ?> base, Iterator<Object> iterator) {
			return predicateBuilder.and(from(predicateBuilder, part, iterator));
		}

		@Override
		protected Predicate<?, ?> or(Predicate<?, ?> base, Predicate<?, ?> criteria) {
			return predicateBuilder.or(criteria);
		}

		@Override
		protected HazelcastQuery complete(Predicate<?, ?> criteria, Sort sort) {
			return new HazelcastQuery(criteria);
		}

		private Predicate<?, ?> from(PredicateBuilder pb, Part part, Iterator<Object> iterator) {

			EntryObject e = pb.getEntryObject();
			e.get(part.getProperty().toDotPath());

			switch (part.getType()) {
				case TRUE:
					return e.equal(true);
				case FALSE:
					return e.equal(false);
				case SIMPLE_PROPERTY:
					return e.equal((Comparable<?>) iterator.next());
				case IS_NULL:
					return e.isNull();
				case GREATER_THAN:
					return e.greaterThan((Comparable<?>) iterator.next());

				default:
					throw new InvalidDataAccessApiUsageException(
							String.format("Found invalid part '%s' in query", part.getType()));
			}
		}

	}

	public static class HazelcastPartTreeQuery<T, ID extends Serializable> extends InMemoryPartTreeQuery<T, ID> {

		public HazelcastPartTreeQuery(QueryMethod queryMethod, HazelcastTemplate inMemoryOperations) {
			super(queryMethod, inMemoryOperations);
		}

		@Override
		public InMemoryQuery getQuery(Object[] parameters) {
			ParametersParameterAccessor accessor = new ParametersParameterAccessor(getQueryMethod().getParameters(),
					parameters);
			PartTree tree = new PartTree(getQueryMethod().getName(), getQueryMethod().getEntityInformation().getJavaType());
			return new HazelcastQueryCreator(tree, accessor).createQuery();
		}

	}
}
