/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.querydsl;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import com.querydsl.core.types.Predicate;

/**
 * {@link RepositoryInvoker} that is aware of a {@link QuerydslPredicateExecutor} and {@link Predicate} to be executed
 * for all flavors of {@code findAll(…)}. All other calls are forwarded to the configured delegate.
 *
 * @author Oliver Gierke
 */
public class QuerydslRepositoryInvokerAdapter implements RepositoryInvoker {

	private final RepositoryInvoker delegate;
	private final QuerydslPredicateExecutor<Object> executor;
	private final Predicate predicate;

	/**
	 * Creates a new {@link QuerydslRepositoryInvokerAdapter} for the given delegate {@link RepositoryInvoker},
	 * {@link QuerydslPredicateExecutor} and Querydsl {@link Predicate}.
	 *
	 * @param delegate must not be {@literal null}.
	 * @param executor must not be {@literal null}.
	 * @param predicate must not be {@literal null}.
	 */
	public QuerydslRepositoryInvokerAdapter(RepositoryInvoker delegate, QuerydslPredicateExecutor<Object> executor,
			Predicate predicate) {

		Assert.notNull(delegate, "Delegate RepositoryInvoker must not be null");
		Assert.notNull(executor, "QuerydslPredicateExecutor must not be null");
		Assert.notNull(predicate, "Predicate must not be null");

		this.delegate = delegate;
		this.executor = executor;
		this.predicate = predicate;
	}

	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return executor.findAll(predicate, pageable);
	}

	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return executor.findAll(predicate, sort);
	}

	@Override
	public boolean hasDeleteMethod() {
		return delegate.hasDeleteMethod();
	}

	@Override
	public boolean hasFindAllMethod() {
		return delegate.hasFindAllMethod();
	}

	@Override
	public boolean hasFindOneMethod() {
		return delegate.hasFindOneMethod();
	}

	@Override
	public boolean hasSaveMethod() {
		return delegate.hasSaveMethod();
	}

	@Override
	public void invokeDeleteById(Object id) {
		delegate.invokeDeleteById(id);
	}

	@Override
	public <T> Optional<T> invokeFindById(Object id) {
		return delegate.invokeFindById(id);
	}

	@Override
	public Optional<Object> invokeQueryMethod(Method method, MultiValueMap<String, ? extends Object> parameters,
			Pageable pageable, Sort sort) {
		return delegate.invokeQueryMethod(method, parameters, pageable, sort);
	}

	@Override
	public <T> T invokeSave(T object) {
		return delegate.invokeSave(object);
	}
}
