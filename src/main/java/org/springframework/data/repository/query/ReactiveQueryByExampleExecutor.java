/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.repository.query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

/**
 * Interface to allow execution of Query by Example {@link Example} instances using a reactive infrastructure.
 *
 * @param <T>
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.0
 */
public interface ReactiveQueryByExampleExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link Example} or {@link Mono#empty()} if none was found.
	 *
	 * @param example must not be {@literal null}.
	 * @return a single entity matching the given {@link Example} or {@link Mono#empty()} if none was found.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException via {@link Mono#error(Throwable)} if the
	 *           example yields more than one result.
	 */
	<S extends T> Mono<S> findOne(Example<S> example);

	/**
	 * Returns all entities matching the given {@link Example}. In case no match could be found {@link Flux#empty()} is
	 * returned.
	 *
	 * @param example must not be {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 */
	<S extends T> Flux<S> findAll(Example<S> example);

	/**
	 * Returns all entities matching the given {@link Example} applying the given {@link Sort}. In case no match could be
	 * found {@link Flux#empty()} is returned.
	 *
	 * @param example must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, must not be {@literal null}.
	 * @return all entities matching the given {@link Example}.
	 */
	<S extends T> Flux<S> findAll(Example<S> example, Sort sort);

	/**
	 * Returns the number of instances matching the given {@link Example}.
	 *
	 * @param example the {@link Example} to count instances for. Must not be {@literal null}.
	 * @return the number of instances matching the {@link Example}.
	 */
	<S extends T> Mono<Long> count(Example<S> example);

	/**
	 * Checks whether the data store contains elements that match the given {@link Example}.
	 *
	 * @param example the {@link Example} to use for the existence check. Must not be {@literal null}.
	 * @return {@literal true} if the data store contains elements that match the given {@link Example}.
	 */
	<S extends T> Mono<Boolean> exists(Example<S> example);
}
