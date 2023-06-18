/*
 * Copyright 2022-2023 the original author or authors.
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

import java.util.List;

import org.springframework.data.domain.Sort;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Interface to allow execution of QueryDsl {@link Predicate} instances. This an extension to
 * {@link QuerydslPredicateExecutor} returning {@link List} instead of {@link Iterable} where applicable.
 *
 * @author Jens Schauder
 * @since 3.0
 * @see QuerydslPredicateExecutor
 */
public interface ListQuerydslPredicateExecutor<T> extends QuerydslPredicateExecutor<T> {

	/**
	 * Returns all entities matching the given {@link Predicate}. In case no match could be found an empty {@link List} is
	 * returned.
	 *
	 * @param predicate must not be {@literal null}.
	 * @return all entities matching the given {@link Predicate}.
	 */
	@Override
	List<T> findAll(Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate} applying the given {@link Sort}. In case no match could
	 * be found an empty {@link List} is returned.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities matching the given {@link Predicate}.
	 */
	@Override
	List<T> findAll(Predicate predicate, Sort sort);

	/**
	 * Returns all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s. In case no
	 * match could be found an empty {@link List} is returned.
	 *
	 * @param predicate must not be {@literal null}.
	 * @param orders the {@link OrderSpecifier}s to sort the results by, must not be {@literal null}.
	 * @return all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s.
	 */
	@Override
	List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	/**
	 * Returns all entities ordered by the given {@link OrderSpecifier}s.
	 *
	 * @param orders the {@link OrderSpecifier}s to sort the results by, must not be {@literal null}.
	 * @return all entities ordered by the given {@link OrderSpecifier}s.
	 */
	@Override
	List<T> findAll(OrderSpecifier<?>... orders);

}
