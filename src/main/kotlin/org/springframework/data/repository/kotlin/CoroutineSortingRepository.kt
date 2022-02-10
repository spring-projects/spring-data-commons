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
import org.springframework.data.domain.Sort
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository

/**
 * Repository fragment to provide methods to retrieve entities using the sorting
 * abstraction.
 *
 * In many cases this should be combined with [CoroutineCrudRepository] or similar or manually added methods to provide CRUD functionality.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 2.3
 * @see Flow
 * @see Sort
 * @see CoroutineCrudRepository
 */
@NoRepositoryBean
interface CoroutineSortingRepository<T, ID> : Repository<T, ID> {

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort must not be null.
	 * @return all entities sorted by the given options.
	 * @throws IllegalArgumentException in case the given [Sort] is null.
	 */
	fun findAll(sort: Sort): Flow<T>
}
