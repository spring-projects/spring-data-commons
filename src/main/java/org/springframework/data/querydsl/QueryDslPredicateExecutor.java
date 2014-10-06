/*
 * Copyright 2011-2014 the original author or authors.
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

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;

/**
 * Interface to allow execution of QueryDsl {@link Predicate} instances.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public interface QueryDslPredicateExecutor<T> {

	/**
	 * Returns a single entity matching the given {@link Predicate} or {@literal null} if none was found.
	 * If the predicate yields more than one result a {@link IncorrectResultSizeDataAccessException} is thrown.
	 * 
	 * @param predicate
	 * @return
	 */
	T findOne(Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate}.
	 * In case no match could be found an empty {@link Iterable} is returned.
	 * 
	 * @param predicate
	 * @return
	 */
	Iterable<T> findAll(Predicate predicate);

	/**
	 * Returns all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s.
	 * In case no match could be found an empty {@link Iterable} is returned.
	 * 
	 * @param predicate
	 * @param orders
	 * @return
	 */
	Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

	/**
	 * Returns a {@link Page} of entities matching the given {@link Predicate}.
	 * In case no match could be found, an empty {@link Page} is returned.
	 * 
	 * @param predicate
	 * @param pageable
	 * @return
	 */
	Page<T> findAll(Predicate predicate, Pageable pageable);

	/**
	 * Returns the number of instances that the given {@link Predicate} will return.
	 * 
	 * @param predicate the {@link Predicate} to count instances for
	 * @return the number of instances
	 */
	long count(Predicate predicate);
}
