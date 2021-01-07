/*
 * Copyright 2019-2021 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Interface to issue queries using Querydsl {@link Predicate} instances. <br />
 * Intended for usage along with the {@link org.springframework.data.repository.reactive.ReactiveCrudRepository}
 * interface to wire in <a href="https://http://www.querydsl.com">Querydsl</a>support.
 *
 * <pre>
 *     <code>
 *
 *  public interface PersonRepository extends ReactiveCrudRepository&lt;Person, String&gt;, ReactiveQuerydslPredicateExecutor&lt;Person&gt; {
 *
 *  }
 *
 *  // ...
 *
 *  personRepository.findOne(QPerson.person.email.eq("t-800&#0064;skynet.io"))
 *      .flatMap(t800 ->
 *      //....
 *
 *     </code>
 * </pre>
 *
 * <strong>IMPORTANT:</strong> Please check the module specific documentation whether or not Querydsl is supported.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.2
 */
public interface ReactiveQuerydslPredicateExecutor<T> {

	/**
	 * Returns a {@link Mono} emitting the entity matching the given {@link Predicate} or {@link Mono#empty()} if none was
	 * found.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return a {@link Mono} emitting a single entity matching the given {@link Predicate} or {@link Mono#empty()} if
	 *         none was found.
	 * @throws IllegalArgumentException if the required parameter is {@literal null}.
	 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if the predicate yields more than one
	 *           result.
	 */
	Mono<T> findOne(Predicate predicate);

	/**
	 * Returns a {@link Flux} emitting all entities matching the given {@link Predicate}. In case no match could be found,
	 * {@link Flux} emits no items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return a {@link Flux} emitting all entities matching the given {@link Predicate} one by one.
	 * @throws IllegalArgumentException if the required parameter is {@literal null}.
	 */
	Flux<T> findAll(Predicate predicate);

	/**
	 * Returns a {@link Flux} emitting all entities matching the given {@link Predicate} applying the given {@link Sort}.
	 * In case no match could be found, {@link Flux} emits no items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return a {@link Flux} emitting all entities matching the given {@link Predicate} one by one.
	 * @throws IllegalArgumentException if one of the required parameters is {@literal null}.
	 */
	Flux<T> findAll(Predicate predicate, Sort sort);

	/**
	 * Returns a {@link Flux} emitting all entities matching the given {@link Predicate} applying the given
	 * {@link OrderSpecifier}s. In case no match could be found, {@link Flux} emits no items.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param orders the {@link OrderSpecifier}s to sort the results by.
	 * @return a {@link Flux} emitting all entities matching the given {@link Predicate} applying the given
	 *         {@link OrderSpecifier}s.
	 * @throws IllegalArgumentException if one of the required parameter is {@literal null}, or contains a {@literal null}
	 *           value.
	 */
	Flux<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	/**
	 * Returns a {@link Flux} emitting all entities ordered by the given {@link OrderSpecifier}s.
	 *
	 * @param orders the {@link OrderSpecifier}s to sort the results by.
	 * @return a {@link Flux} emitting all entities ordered by the given {@link OrderSpecifier}s.
	 * @throws IllegalArgumentException one of the {@link OrderSpecifier OrderSpecifiers} is {@literal null}.
	 */
	Flux<T> findAll(OrderSpecifier<?>... orders);

	/**
	 * Returns a {@link Mono} emitting the number of instances matching the given {@link Predicate}.
	 *
	 * @param predicate the {@link Predicate} to count instances for, must not be {@literal null}.
	 * @return a {@link Mono} emitting the number of instances matching the {@link Predicate} or {@code 0} if none found.
	 * @throws IllegalArgumentException if the required parameter is {@literal null}.
	 */
	Mono<Long> count(Predicate predicate);

	/**
	 * Checks whether the data store contains elements that match the given {@link Predicate}.
	 *
	 * @param predicate the {@link Predicate} to use for the existence check, must not be {@literal null}.
	 * @return a {@link Mono} emitting {@literal true} if the data store contains elements that match the given
	 *         {@link Predicate}, {@literal false} otherwise.
	 * @throws IllegalArgumentException if the required parameter is {@literal null}.
	 */
	Mono<Boolean> exists(Predicate predicate);
}
