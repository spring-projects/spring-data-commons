/*
 * Copyright 2017-2018 the original author or authors.
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

import io.reactivex.Flowable;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Extension of {@link RxJava2CrudRepository} to provide additional methods to retrieve entities using the sorting
 * abstraction.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see Sort
 * @see Flowable
 * @see RxJava2CrudRepository
 */
@NoRepositoryBean
public interface RxJava2SortingRepository<T, ID> extends RxJava2CrudRepository<T, ID> {

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort must not be {@literal null}.
	 * @return all entities sorted by the given options.
	 */
	Flowable<T> findAll(Sort sort);
}
