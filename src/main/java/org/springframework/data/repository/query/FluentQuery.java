/*
 * Copyright 2021-2022 the original author or authors.
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
package org.springframework.data.repository.query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * Fluent interface to define and run a query along with projection and sorting and. Instances of {@link FluentQuery}
 * are immutable.
 *
 * @author Mark Paluch
 * @since 2.6
 */
public interface FluentQuery<T> {

	/**
	 * Define the sort order.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if resultType is {@code null}.
	 */
	FluentQuery<T> sortBy(Sort sort);

	/**
	 * Define the target type the result should be mapped to. Skip this step if you are only interested in the original
	 * domain type.
	 *
	 * @param resultType must not be {@code null}.
	 * @param <R> result type.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if resultType is {@code null}.
	 */
	<R> FluentQuery<R> as(Class<R> resultType);

	/**
	 * Define which properties or property paths to include in the query.
	 *
	 * @param properties must not be {@code null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if fields is {@code null}.
	 */
	default FluentQuery<T> project(String... properties) {
		return project(Arrays.asList(properties));
	}

	/**
	 * Define which properties or property paths to include in the query.
	 *
	 * @param properties must not be {@code null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if fields is {@code null}.
	 */
	FluentQuery<T> project(Collection<String> properties);

	/**
	 * Fetchable extension {@link FluentQuery} allowing to materialize results from the underlying query.
	 *
	 * @author Mark Paluch
	 * @since 2.6
	 */
	interface FetchableFluentQuery<T> extends FluentQuery<T> {

		@Override
		FetchableFluentQuery<T> sortBy(Sort sort);

		@Override
		<R> FetchableFluentQuery<R> as(Class<R> resultType);

		@Override
		default FetchableFluentQuery<T> project(String... properties) {
			return project(Arrays.asList(properties));
		}

		@Override
		FetchableFluentQuery<T> project(Collection<String> properties);

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@link Optional#empty()} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		default Optional<T> one() {
			return Optional.ofNullable(oneValue());
		}

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@literal null} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		@Nullable
		T oneValue();

		/**
		 * Get the first or no result.
		 *
		 * @return {@link Optional#empty()} if no match found.
		 */
		default Optional<T> first() {
			return Optional.ofNullable(firstValue());
		}

		/**
		 * Get the first or no result.
		 *
		 * @return {@literal null} if no match found.
		 */
		@Nullable
		T firstValue();

		/**
		 * Get all matching elements.
		 *
		 * @return
		 */
		List<T> all();

		/**
		 * Get a page of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort} if
		 *          the {@link Sort} object is not {@link Sort#isUnsorted()}.
		 * @return
		 */
		Page<T> page(Pageable pageable);

		/**
		 * Stream all matching elements.
		 *
		 * @return a {@link Stream} wrapping cursors that need to be closed.
		 */
		Stream<T> stream();

		/**
		 * Get the number of matching elements.
		 *
		 * @return total number of matching elements.
		 */
		long count();

		/**
		 * Check for the presence of matching elements.
		 *
		 * @return {@literal true} if at least one matching element exists.
		 */
		boolean exists();
	}

	/**
	 * Reactive extension {@link FluentQuery} allowing to materialize results from the underlying query.
	 *
	 * @author Mark Paluch
	 * @since 2.6
	 */
	interface ReactiveFluentQuery<T> extends FluentQuery<T> {

		@Override
		ReactiveFluentQuery<T> sortBy(Sort sort);

		@Override
		<R> ReactiveFluentQuery<R> as(Class<R> resultType);

		@Override
		default ReactiveFluentQuery<T> project(String... properties) {
			return project(Arrays.asList(properties));
		}

		@Override
		ReactiveFluentQuery<T> project(Collection<String> properties);

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@code null} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		Mono<T> one();

		/**
		 * Get the first or no result.
		 *
		 * @return {@code null} if no match found.
		 */
		Mono<T> first();

		/**
		 * Get all matching elements.
		 *
		 * @return
		 */
		Flux<T> all();

		/**
		 * Get a page of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort} if
		 *          the {@link Sort} object is not {@link Sort#isUnsorted()}.
		 * @return
		 */
		Mono<Page<T>> page(Pageable pageable);

		/**
		 * Get the number of matching elements.
		 *
		 * @return total number of matching elements.
		 */
		Mono<Long> count();

		/**
		 * Check for the presence of matching elements.
		 *
		 * @return {@literal true} if at least one matching element exists.
		 */
		Mono<Boolean> exists();

	}
}
