/*
 * Copyright 2016-2025 the original author or authors.
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

import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Interface to allow execution of Query by Example {@link Example} instances.
 *
 * @param <T>
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Diego Krupitza
 * @since 1.12
 * @see ListQueryByExampleExecutor
 */
public interface QueryByExampleExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link Example} or {@link Optional#empty()} if none was found.
	 *
	 * @param example must not be {@literal null}.
	 * @return a single entity matching the given {@link Example} or {@link Optional#empty()} if none was found.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if the Example yields more than one result.
	 */
	<S extends T> Optional<S> findOne(Example<S> example);

	/**
	 * Returns all entities matching the given {@link Example}. In case no match could be found an empty {@link Iterable}
	 * is returned.
	 *
	 * @param example must not be {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 */
	<S extends T> Iterable<S> findAll(Example<S> example);

	/**
	 * Returns all entities matching the given {@link Example} applying the given {@link Sort}. In case no match could be
	 * found an empty {@link Iterable} is returned.
	 *
	 * @param example must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 * @since 1.10
	 */
	<S extends T> Iterable<S> findAll(Example<S> example, Sort sort);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Example}. In case no match could be found, an empty
	 * {@link Page} is returned.
	 *
	 * @param example must not be {@literal null}.
	 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
	 *          {@literal null}.
	 * @return a {@link Page} of entities matching the given {@link Example}.
	 */
	<S extends T> Page<S> findAll(Example<S> example, Pageable pageable);

	/**
	 * Returns the number of instances matching the given {@link Example}.
	 *
	 * @param example the {@link Example} to count instances for. Must not be {@literal null}.
	 * @return the number of instances matching the {@link Example}.
	 */
	<S extends T> long count(Example<S> example);

	/**
	 * Checks whether the data store contains elements that match the given {@link Example}.
	 *
	 * @param example the {@link Example} to use for the existence check. Must not be {@literal null}.
	 * @return {@literal true} if the data store contains elements that match the given {@link Example}.
	 */
	<S extends T> boolean exists(Example<S> example);

	/**
	 * Returns entities matching the given {@link Example} applying the {@link Function queryFunction} that defines the
	 * query and its result type.
	 * <p>
	 * The query object used with {@code queryFunction} is only valid inside the {@code findBy(…)} method call. This
	 * requires the query function to return a query result and not the {@link FluentQuery} object itself to ensure the
	 * query is executed inside the {@code findBy(…)} method.
	 *
	 * @param example must not be {@literal null}.
	 * @param queryFunction the query function defining projection, sorting, and the result type
	 * @return all entities matching the given {@link Example}.
	 * @since 2.6
	 */
	<S extends T, R extends @Nullable Object> R findBy(Example<S> example,
			Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);
}
