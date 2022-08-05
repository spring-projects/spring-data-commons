/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.data.repository.reactive;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Interface for generic CRUD operations on a repository for a specific type. This repository follows reactive paradigms
 * and uses RxJava 3 types.
 * <p>
 * Save and delete operations with entities that have a version attribute trigger an {@code onError} with a {@link org.springframework.dao.OptimisticLockingFailureException} when they encounter a different version value in the persistence store than in the entity passed as an argument.
 * </p>
 * <p>
 * Other delete operations that only receive ids or entities without version attribute do not trigger an error when no matching data is found in the persistence store.
 * </p>
 *
 * @author Mark Paluch
 * @since 2.4
 * @see Maybe
 * @see Single
 * @see Flowable
 * @see Completable
 */
@NoRepositoryBean
public interface RxJava3CrudRepository<T, ID> extends Repository<T, ID> {

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity must not be {@literal null}.
	 * @return {@link Single} emitting the saved entity.
	 * @throws IllegalArgumentException in case the given {@literal entity} is {@literal null}.
	 */
	<S extends T> Single<S> save(S entity);

	/**
	 * Saves all given entities.
	 *
	 * @param entities must not be {@literal null}.
	 * @return {@link Flowable} emitting the saved entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable entities} or one of its entities is
	 *           {@literal null}.
	 */
	<S extends T> Flowable<S> saveAll(Iterable<S> entities);

	/**
	 * Saves all given entities.
	 *
	 * @param entityStream must not be {@literal null}.
	 * @return {@link Flowable} emitting the saved entities.
	 * @throws IllegalArgumentException in case the given {@link Flowable entityStream} is {@literal null}.
	 */
	<S extends T> Flowable<S> saveAll(Flowable<S> entityStream);

	/**
	 * Retrieves an entity by its id.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Maybe} emitting the entity with the given id or {@link Maybe#empty()} if none found.
	 * @throws IllegalArgumentException in case the given {@literal id} is {@literal null}.
	 */
	Maybe<T> findById(ID id);

	/**
	 * Retrieves an entity by its id supplied by a {@link Single}.
	 *
	 * @param id must not be {@literal null}. Uses the first emitted element to perform the find-query.
	 * @return {@link Maybe} emitting the entity with the given id or {@link Maybe#empty()} if none found.
	 * @throws IllegalArgumentException in case the given {@link Single id} is {@literal null}.
	 */
	Maybe<T> findById(Single<ID> id);

	/**
	 * Returns whether an entity with the given {@literal id} exists.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Single} emitting {@literal true} if an entity with the given id exists, {@literal false} otherwise.
	 * @throws IllegalArgumentException in case the given {@literal id} is {@literal null}.
	 */
	Single<Boolean> existsById(ID id);

	/**
	 * Returns whether an entity with the given id, supplied by a {@link Single}, exists.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Single} emitting {@literal true} if an entity with the given id exists, {@literal false} otherwise.
	 * @throws IllegalArgumentException in case the given {@link Single id} is {@literal null}.
	 */
	Single<Boolean> existsById(Single<ID> id);

	/**
	 * Returns all instances of the type.
	 *
	 * @return {@link Flowable} emitting all entities.
	 */
	Flowable<T> findAll();

	/**
	 * Returns all instances of the type {@code T} with the given IDs.
	 * <p>
	 * If some or all ids are not found, no entities are returned for these IDs.
	 * <p>
	 * Note that the order of elements in the result is not guaranteed.
	 *
	 * @param ids must not be {@literal null} nor contain any {@literal null} values.
	 * @return {@link Flowable} emitting the found entities. The size can be equal or less than the number of given
	 *         {@literal ids}.
	 * @throws IllegalArgumentException in case the given {@link Iterable ids} or one of its items is {@literal null}.
	 */
	Flowable<T> findAllById(Iterable<ID> ids);

	/**
	 * Returns all instances of the type {@code T} with the given IDs supplied by a {@link Flowable}.
	 * <p>
	 * If some or all ids are not found, no entities are returned for these IDs.
	 * <p>
	 * Note that the order of elements in the result is not guaranteed.
	 *
	 * @param idStream must not be {@literal null}.
	 * @return {@link Flowable} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Flowable idStream} is {@literal null}.
	 */
	Flowable<T> findAllById(Flowable<ID> idStream);

	/**
	 * Returns the number of entities available.
	 *
	 * @return {@link Single} emitting the number of entities.
	 */
	Single<Long> count();

	/**
	 * Deletes the entity with the given id.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Completable} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@literal id} is {@literal null}.
	 */
	Completable deleteById(ID id);

	/**
	 * Deletes a given entity.
	 *
	 * @param entity must not be {@literal null}.
	 * @return {@link Completable} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given entity is {@literal null}.
	 */
	Completable delete(T entity);

	/**
	 * Deletes all instances of the type {@code T} with the given IDs.
	 *
	 * @param ids must not be {@literal null}.
	 * @return {@link Completable} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@literal ids} or one of its elements is {@literal null}.
	 *           {@literal null}.
	 * @since 2.5
	 */
	Completable deleteAllById(Iterable<? extends ID> ids);

	/**
	 * Deletes the given entities.
	 *
	 * @param entities must not be {@literal null}.
	 * @return {@link Completable} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@link Iterable entities} or one of its entities is
	 *           {@literal null}.
	 */
	Completable deleteAll(Iterable<? extends T> entities);

	/**
	 * Deletes the given entities supplied by a {@link Flowable}.
	 *
	 * @param entityStream must not be {@literal null}.
	 * @return {@link Completable} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@link Flowable entityStream} is {@literal null}.
	 */
	Completable deleteAll(Flowable<? extends T> entityStream);

	/**
	 * Deletes all entities managed by the repository.
	 *
	 * @return {@link Completable} signaling when operation has completed.
	 */
	Completable deleteAll();
}
