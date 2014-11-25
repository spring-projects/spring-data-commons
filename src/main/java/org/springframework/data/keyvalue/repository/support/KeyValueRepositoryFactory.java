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

import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.QueryDslKeyValueRepository;
import org.springframework.data.keyvalue.repository.SimpleKeyValueRepository;
import org.springframework.data.keyvalue.repository.query.KeyValuePartTreeQuery;
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
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link RepositoryFactorySupport} specific of handing
 * {@link org.springframework.data.keyvalue.repository.KeyValueRepository}.
 * 
 * @author Christoph Strobl
 * @since 1.10
 */
public class KeyValueRepositoryFactory extends RepositoryFactorySupport {

	private static final Class<SpelQueryCreator> DEFAULT_QUERY_CREATOR = SpelQueryCreator.class;

	private final KeyValueOperations keyValueOperations;
	private final MappingContext<?, ?> context;
	private final Class<? extends AbstractQueryCreator<?, ?>> queryCreator;

	/**
	 * Creates a new {@link KeyValueRepositoryFactory} for the given {@link KeyValueOperations}.
	 * 
	 * @param keyValueOperations must not be {@literal null}.
	 */
	public KeyValueRepositoryFactory(KeyValueOperations keyValueOperations) {
		this(keyValueOperations, DEFAULT_QUERY_CREATOR);
	}

	/**
	 * Creates a new {@link KeyValueRepositoryFactory} for the given {@link KeyValueOperations} and
	 * {@link AbstractQueryCreator}-type.
	 * 
	 * @param keyValueOperations must not be {@literal null}.
	 * @param queryCreator defaulted to {@link #DEFAULT_QUERY_CREATOR} if {@literal null}.
	 */
	public KeyValueRepositoryFactory(KeyValueOperations keyValueOperations,
			Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {

		Assert.notNull(keyValueOperations, "KeyValueOperations must not be null!");
		Assert.notNull(queryCreator, "Query creator type must not be null!");

		this.queryCreator = queryCreator;
		this.keyValueOperations = keyValueOperations;
		this.context = keyValueOperations.getMappingContext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		PersistentEntity<T, ?> entity = (PersistentEntity<T, ?>) context.getPersistentEntity(domainClass);
		PersistentEntityInformation<T, ID> entityInformation = new PersistentEntityInformation<T, ID>(entity);

		return entityInformation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Object getTargetRepository(RepositoryMetadata metadata) {

		EntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());

		if (ClassUtils.isAssignable(QueryDslPredicateExecutor.class, metadata.getRepositoryInterface())) {
			return new QueryDslKeyValueRepository(entityInformation, keyValueOperations);
		}

		return new SimpleKeyValueRepository(entityInformation, keyValueOperations);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return isQueryDslRepository(metadata.getRepositoryInterface()) ? QueryDslKeyValueRepository.class
				: SimpleKeyValueRepository.class;
	}

	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 * @return
	 */
	private static boolean isQueryDslRepository(Class<?> repositoryInterface) {
		return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return new KeyValueQueryLookupStrategy(key, evaluationContextProvider, this.keyValueOperations, this.queryCreator);
	}

	/**
	 * @author Christoph Strobl
	 * @author Oliver Gierke
	 * @since 1.10
	 */
	private static class KeyValueQueryLookupStrategy implements QueryLookupStrategy {

		private EvaluationContextProvider evaluationContextProvider;
		private KeyValueOperations keyValueOperations;

		private Class<? extends AbstractQueryCreator<?, ?>> queryCreator;

		/**
		 * Creates a new {@link KeyValueQueryLookupStrategy} for the given {@link Key}, {@link EvaluationContextProvider},
		 * {@link KeyValueOperations} and query creator type.
		 * <p>
		 * TODO: Key is not considered. Should it?
		 * 
		 * @param key
		 * @param evaluationContextProvider must not be {@literal null}.
		 * @param keyValueOperations must not be {@literal null}.
		 * @param queryCreator must not be {@literal null}.
		 */
		public KeyValueQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider,
				KeyValueOperations keyValueOperations, Class<? extends AbstractQueryCreator<?, ?>> queryCreator) {

			Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
			Assert.notNull(keyValueOperations, "KeyValueOperations must not be null!");
			Assert.notNull(queryCreator, "Query creator type must not be null!");

			this.evaluationContextProvider = evaluationContextProvider;
			this.keyValueOperations = keyValueOperations;
			this.queryCreator = queryCreator;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.repository.core.NamedQueries)
		 */
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, NamedQueries namedQueries) {

			QueryMethod queryMethod = new QueryMethod(method, metadata);
			return new KeyValuePartTreeQuery(queryMethod, evaluationContextProvider, this.keyValueOperations,
					this.queryCreator);
		}
	}
}
