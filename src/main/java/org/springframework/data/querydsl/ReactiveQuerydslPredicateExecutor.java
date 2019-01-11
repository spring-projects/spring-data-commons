/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.querydsl;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Interface to issue queries using Querydsl {@link Predicate} instances.
 *
 * @author Mark Paluch
 * @since 2.2
 */
public interface ReactiveQuerydslPredicateExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link Predicate} or {@link Mono#empty()} if none was found.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return a single entity matching the given {@link Predicate} or {@link Mono#empty()} if none was found.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if the predicate yields more than one
	 *           result.
	 */
	Mono<T> findOne(Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate}. In case no match could be found, {@link Flux} emits no
	 * items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return all entities matching the given {@link Predicate}.
	 */
	Flux<T> findAll(Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate} applying the given {@link Sort}. In case no match could
	 * be found, {@link Flux} emits no items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#empty()}, must not be
	 *          {@literal null}.
	 * @return all entities matching the given {@link Predicate}.
	 */
	Flux<T> findAll(Predicate predicate, Sort sort);

	/**
	 * Returns all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s. In case no
	 * match could be found, {@link Flux} emits no items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param orders the {@link OrderSpecifier}s to sort the results by.
	 * @return all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s.
	 */
	Flux<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	/**
	 * Returns all entities ordered by the given {@link OrderSpecifier}s.
	 *
	 * @param orders the {@link OrderSpecifier}s to sort the results by.
	 * @return all entities ordered by the given {@link OrderSpecifier}s.
	 */
	Flux<T> findAll(OrderSpecifier<?>... orders);

	/**
	 * Returns the number of instances matching the given {@link Predicate}.
	 *
	 * @param predicate the {@link Predicate} to count instances for, must not be {@literal null}.
	 * @return the number of instances matching the {@link Predicate}.
	 */
	Mono<Long> count(Predicate predicate);

	/**
	 * Checks whether the data store contains elements that match the given {@link Predicate}.
	 *
	 * @param predicate the {@link Predicate} to use for the existence check, must not be {@literal null}.
	 * @return {@literal true} if the data store contains elements that match the given {@link Predicate}.
	 */
	Mono<Boolean> exists(Predicate predicate);
}
