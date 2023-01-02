/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.core.support;

import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.mockito.Mockito;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Mark Paluch
 */
public class DummyReactiveRepositoryFactory extends ReactiveRepositoryFactorySupport {

	public final DummyRepositoryFactory.MyRepositoryQuery queryOne = mock(DummyRepositoryFactory.MyRepositoryQuery.class);
	public final RepositoryQuery queryTwo = mock(RepositoryQuery.class);
	final QueryLookupStrategy strategy = mock(QueryLookupStrategy.class);

	@SuppressWarnings("unchecked") private final QuerydslPredicateExecutor<Object> querydsl = mock(
			QuerydslPredicateExecutor.class);
	private final Object repository;

	public DummyReactiveRepositoryFactory(Object repository) {

		this.repository = repository;

		when(strategy.resolveQuery(Mockito.any(Method.class), Mockito.any(RepositoryMetadata.class),
				Mockito.any(ProjectionFactory.class), Mockito.any(NamedQueries.class))).thenReturn(queryOne);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return mock(EntityInformation.class);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation information) {
		return repository;
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return repository.getClass();
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(strategy);
	}

	@Override
	protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		var fragments = super.getRepositoryFragments(metadata);

		return QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface()) //
				? fragments.append(RepositoryComposition.RepositoryFragments.just(querydsl)) //
				: fragments;
	}
}
