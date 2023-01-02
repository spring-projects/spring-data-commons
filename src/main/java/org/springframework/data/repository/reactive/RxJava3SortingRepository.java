/*
 * Copyright 2020-2023 the original author or authors.
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

import io.reactivex.rxjava3.core.Flowable;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Repository fragment to provide methods to retrieve entities using the sorting abstraction. In many cases this
 * should be combined with {@link RxJava3CrudRepository} or a similar interface to provide CRUD functionality.
 * 
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 2.4
 * @see Sort
 * @see Flowable
 * @see RxJava3CrudRepository
 */
@NoRepositoryBean
public interface RxJava3SortingRepository<T, ID> extends Repository<T, ID> {

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities sorted by the given options.
	 */
	Flowable<T> findAll(Sort sort);
}
