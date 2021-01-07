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
package org.springframework.data.repository.kotlin

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import reactor.core.publisher.Mono

/**
 * Interface for generic CRUD operations using Kotlin Coroutines on a repository for a specific type.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.3
 * @see Flow
 */
@NoRepositoryBean
interface CoroutineCrudRepository<T, ID> : Repository<T, ID> {

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity must not be null.
	 * @return  the saved entity.
	 * @throws IllegalArgumentException in case the given entity is null.
	 */
	suspend fun <S : T> save(entity: S): T

	/**
	 * Saves all given entities.
	 *
	 * @param entities must not be null.
	 * @return [Flow] emitting the saved entities.
	 * @throws IllegalArgumentException in case the given [entities][Flow] or one of its entities is
	 * null.
	 */
	fun <S : T> saveAll(entities: Iterable<S>): Flow<S>

	/**
	 * Saves all given entities.
	 *
	 * @param entityStream must not be null.
	 * @return [Flow] emitting the saved entities.
	 * @throws IllegalArgumentException in case the given [entityStream][Flow] is null.
	 */
	fun <S : T> saveAll(entityStream: Flow<S>): Flow<S>

	/**
	 * Retrieves an entity by its id.
	 *
	 * @param id must not be null.
	 * @return [Mono] emitting the entity with the given id or empty if none found.
	 * @throws IllegalArgumentException in case the given id is null.
	 */
	suspend fun findById(id: ID): T?

	/**
	 * Returns whether an entity with the given id exists.
	 *
	 * @param id must not be null.
	 * @return true if an entity with the given id exists, false otherwise.
	 * @throws IllegalArgumentException in case the given id is null.
	 */
	suspend fun existsById(id: ID): Boolean

	/**
	 * Returns all instances of the type.
	 *
	 * @return [Flow] emitting all entities.
	 */
	fun findAll(): Flow<T>

	/**
	 * Returns all instances of the type `T` with the given IDs.
	 * If some or all ids are not found, no entities are returned for these IDs.
	 * Note that the order of elements in the result is not guaranteed.
	 *
	 * @param ids must not be null nor contain any null values.
	 * @return [Flow] emitting the found entities. The size can be equal or less than the number of given
	 * ids.
	 * @throws IllegalArgumentException in case the given [ids][Iterable] or one of its items is null.
	 */
	fun findAllById(ids: Iterable<ID>): Flow<T>

	/**
	 * Returns all instances of the type `T` with the given IDs.
	 * If some or all ids are not found, no entities are returned for these IDs.
	 * Note that the order of elements in the result is not guaranteed.
	 *
	 * @param ids must not be null nor contain any null values.
	 * @return [Flow] emitting the found entities. The size can be equal or less than the number of given
	 * ids.
	 * @throws IllegalArgumentException in case the given [ids][Iterable] or one of its items is null.
	 */
	fun findAllById(ids: Flow<ID>): Flow<T>

	/**
	 * Returns the number of entities available.
	 *
	 * @return number of entities.
	 */
	suspend fun count(): Long

	/**
	 * Deletes the entity with the given id.
	 *
	 * @param id must not be null.
	 * @throws IllegalArgumentException in case the given id is null.
	 */
	suspend fun deleteById(id: ID)

	/**
	 * Deletes a given entity.
	 *
	 * @param entity must not be null.
	 * @throws IllegalArgumentException in case the given entity is null.
	 */
	suspend fun delete(entity: T)

	/**
	 * Deletes the given entities.
	 *
	 * @param entities must not be null.
	 * @throws IllegalArgumentException in case the given [entities][Iterable] or one of its entities is
	 * null.
	 */
	suspend fun deleteAll(entities: Iterable<T>)

	/**
	 * Deletes all given entities.
	 *
	 * @param entityStream must not be null.
	 * @throws IllegalArgumentException in case the given [entityStream][Flow] is null.
	 */
	suspend fun <S : T> deleteAll(entityStream: Flow<S>)

	/**
	 * Deletes all entities managed by the repository.
	 */
	suspend fun deleteAll()
}
