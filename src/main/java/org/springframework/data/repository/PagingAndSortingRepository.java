/*
 * Copyright 2008-2023 the original author or authors.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Repository fragment to provide methods to retrieve entities using the pagination and sorting abstraction. In many
 * cases this will be combined with {@link CrudRepository} or similar or with manually added methods to provide CRUD
 * functionality.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @see Sort
 * @see Pageable
 * @see Page
 * @see CrudRepository
 */
@NoRepositoryBean
public interface PagingAndSortingRepository<T, ID> extends Repository<T, ID> {

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, can be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return all entities sorted by the given options
	 */
	Iterable<T> findAll(Sort sort);

	/**
	 * Returns a {@link Page} of entities meeting the paging restriction provided in the {@link Pageable} object.
	 *
	 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
	 *          {@literal null}.
	 * @return a page of entities
	 */
	Page<T> findAll(Pageable pageable);
}
