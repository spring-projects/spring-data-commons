/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.repository.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.reactivestreams.Publisher;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Interface for generic CRUD operations on a repository for a specific type. This repository follows reactive paradigms
 * and uses Project Reactor types which are built on top of Reactive Streams.
 *
 * @author Mark Paluch
 * @author Christph Strobl
 * @since 2.0
 * @see Mono
 * @see Flux
 */
@NoRepositoryBean
public interface ReactiveCrudRepository<T, ID> extends Repository<T, ID> {

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed the
	 * entity instance completely.
	 *
	 * @param entity must not be {@literal null}.
	 * @return {@link Mono} emitting the saved entity.
	 * @throws IllegalArgumentException in case the given {@code entity} is {@literal null}.
	 */
	<S extends T> Mono<S> save(S entity);

	/**
	 * Saves all given entities.
	 *
	 * @param entities must not be {@literal null}.
	 * @return {@link Flux} emitting the saved entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code entities} is {@literal null}.
	 */
	<S extends T> Flux<S> saveAll(Iterable<S> entities);

	/**
	 * Saves all given entities.
	 *
	 * @param entityStream must not be {@literal null}.
	 * @return {@link Flux} emitting the saved entities.
	 * @throws IllegalArgumentException in case the given {@code Publisher} {@code entityStream} is {@literal null}.
	 */
	<S extends T> Flux<S> saveAll(Publisher<S> entityStream);

	/**
	 * Retrieves an entity by its id.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Mono} emitting the entity with the given id or {@link Mono#empty()} if none found.
	 * @throws IllegalArgumentException in case the given {@code id} is {@literal null}.
	 */
	Mono<T> findById(ID id);

	/**
	 * Retrieves an entity by its id supplied by a {@link Publisher}.
	 *
	 * @param id must not be {@literal null}. Uses the first emitted element to perform the find-query.
	 * @return {@link Mono} emitting the entity with the given id or {@link Mono#empty()} if none found.
	 * @throws IllegalArgumentException in case the given {@link Publisher} {@code id} is {@literal null}.
	 */
	Mono<T> findById(Publisher<ID> id);

	/**
	 * Returns whether an entity with the id exists.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Mono} emitting {@literal true} if an entity with the given id exists, {@literal false} otherwise.
	 * @throws IllegalArgumentException in case the given {@code id} is {@literal null}.
	 */
	Mono<Boolean> existsById(ID id);

	/**
	 * Returns whether an entity with the given id, supplied by a {@link Publisher}, exists. Uses the first emitted
	 * element to perform the exists-query.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Mono} emitting {@literal true} if an entity with the given id exists, {@literal false} otherwise
	 * @throws IllegalArgumentException in case the given {@link Publisher} {@code id} is {@literal null}.
	 */
	Mono<Boolean> existsById(Publisher<ID> id);

	/**
	 * Returns all instances of the type.
	 *
	 * @return {@link Flux} emitting all entities.
	 */
	Flux<T> findAll();

	/**
	 * Returns all instances with the given IDs.
	 *
	 * @param ids must not be {@literal null}.
	 * @return {@link Flux} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code ids} is {@literal null}.
	 */
	Flux<T> findAllById(Iterable<ID> ids);

	/**
	 * Returns all instances of the type with the given IDs supplied by a {@link Publisher}.
	 *
	 * @param idStream must not be {@literal null}.
	 * @return {@link Flux} emitting the found entities.
	 * @throws IllegalArgumentException in case the given {@link Publisher} {@code idStream} is {@literal null}.
	 */
	Flux<T> findAllById(Publisher<ID> idStream);

	/**
	 * Returns the number of entities available.
	 *
	 * @return {@link Mono} emitting the number of entities.
	 */
	Mono<Long> count();

	/**
	 * Deletes the entity with the given id.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Mono} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@code id} is {@literal null}.
	 */
	Mono<Void> deleteById(ID id);

	/**
	 * Deletes the entity with the given id supplied by a {@link Publisher}.
	 *
	 * @param id must not be {@literal null}.
	 * @return {@link Mono} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@link Publisher} {@code id} is {@literal null}.
	 */
	Mono<Void> deleteById(Publisher<ID> id);

	/**
	 * Deletes a given entity.
	 *
	 * @param entity must not be {@literal null}.
	 * @return {@link Mono} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given entity is {@literal null}.
	 */
	Mono<Void> delete(T entity);

	/**
	 * Deletes the given entities.
	 *
	 * @param entities must not be {@literal null}.
	 * @return {@link Mono} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@link Iterable} {@code entities} is {@literal null}.
	 */
	Mono<Void> deleteAll(Iterable<? extends T> entities);

	/**
	 * Deletes the given entities supplied by a {@link Publisher}.
	 *
	 * @param entityStream must not be {@literal null}.
	 * @return {@link Mono} signaling when operation has completed.
	 * @throws IllegalArgumentException in case the given {@link Publisher} {@code entityStream} is {@literal null}.
	 */
	Mono<Void> deleteAll(Publisher<? extends T> entityStream);

	/**
	 * Deletes all entities managed by the repository.
	 *
	 * @return {@link Mono} signaling when operation has completed.
	 */
	Mono<Void> deleteAll();
}
