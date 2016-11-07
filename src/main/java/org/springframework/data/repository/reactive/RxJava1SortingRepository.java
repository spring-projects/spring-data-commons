/*
 * Copyright 2016 the original author or authors.
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

import java.io.Serializable;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

import rx.Observable;
import rx.Single;

/**
 * Extension of {@link RxJava1CrudRepository} to provide additional methods to retrieve entities using the sorting
 * abstraction.
 *
 * @author Mark Paluch
 * @since 2.0
 * @see Sort
 * @see Single
 * @see Observable
 * @see RxJava1CrudRepository
 */
@NoRepositoryBean
public interface RxJava1SortingRepository<T, ID extends Serializable> extends RxJava1CrudRepository<T, ID> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAll()
	 */
	@Override
	Observable<T> findAll();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAll(java.lang.Iterable)
	 */
	@Override
	Observable<T> findAll(Iterable<ID> ids);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#findAll(org.reactivestreams.Publisher)
	 */
	@Override
	Observable<T> findAll(Observable<ID> idStream);

	/**
	 * Returns all entities sorted by the given options.
	 *
	 * @param sort
	 * @return all entities sorted by the given options
	 */
	Observable<T> findAll(Sort sort);
}
