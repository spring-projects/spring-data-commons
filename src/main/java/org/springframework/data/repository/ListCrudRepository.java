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
package org.springframework.data.repository;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Interface for generic CRUD operations on a repository for a specific type. This an extension to
 * {@link CrudRepository} returning {@link List} instead of {@link Iterable} where applicable.
 *
 * @author Jens Schauder
 * @see CrudRepository
 * @since 3.0
 */
@NoRepositoryBean
public interface ListCrudRepository<T, ID> extends CrudRepository<T, ID> {

	/**
	 * Saves all given entities.
	 *
	 * @param entities must not be {@literal null} nor must it contain {@literal null}.
	 * @return the saved entities; will never be {@literal null}. The returned {@literal Iterable} will have the same size
	 *         as the {@literal Iterable} passed as an argument.
	 * @throws IllegalArgumentException in case the given {@link Iterable entities} or one of its entities is
	 *           {@literal null}.
	 * @throws OptimisticLockingFailureException when at least one entity uses optimistic locking and has a version
	 *           attribute with a different value from that found in the persistence store. Also thrown if at least one
	 *           entity is assumed to be present but does not exist in the database.
	 */
	<S extends T> List<S> saveAll(Iterable<S> entities);

	/**
	 * Returns all instances of the type.
	 *
	 * @return all entities
	 */
	List<T> findAll();

	/**
	 * Returns all instances of the type {@code T} with the given IDs.
	 * <p>
	 * If some or all ids are not found, no entities are returned for these IDs.
	 * <p>
	 * Note that the order of elements in the result is not guaranteed.
	 *
	 * @param ids must not be {@literal null} nor contain any {@literal null} values.
	 * @return guaranteed to be not {@literal null}. The size can be equal or less than the number of given
	 *         {@literal ids}.
	 * @throws IllegalArgumentException in case the given {@link Iterable ids} or one of its items is {@literal null}.
	 */
	List<T> findAllById(Iterable<ID> ids);

}
