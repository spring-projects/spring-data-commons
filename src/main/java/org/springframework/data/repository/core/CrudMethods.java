/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.repository.core;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Meta-information about the CRUD methods of a repository.
 *
 * @author Oliver Gierke
 * @since 1.6
 */
public interface CrudMethods {

	/**
	 * Returns the method to be used for saving entities. Usually signature compatible to
	 * {@link CrudRepository#save(Object)}.
	 *
	 * @return the method to save entities or {@link Optional#empty()} if none exposed.
	 * @see #hasSaveMethod()
	 */
	Optional<Method> getSaveMethod();

	/**
	 * Returns whether the repository exposes a save method at all.
	 *
	 * @return
	 */
	boolean hasSaveMethod();

	/**
	 * Returns the find all method of the repository. Implementations should prefer more detailed methods like
	 * {@link PagingAndSortingRepository}'s taking a {@link Pageable} or {@link Sort} instance.
	 *
	 * @return the find all method of the repository or {@link Optional#empty()} if not available.
	 * @see #hasFindAllMethod()
	 */
	Optional<Method> getFindAllMethod();

	/**
	 * Returns whether the repository exposes a find all method at all.
	 *
	 * @return
	 */
	boolean hasFindAllMethod();

	/**
	 * Returns the find one method of the repository. Usually signature compatible to
	 * {@link CrudRepository#findById(Object)}
	 *
	 * @return the find one method of the repository or {@link Optional#empty()} if not available.
	 * @see #hasFindOneMethod()
	 */
	Optional<Method> getFindOneMethod();

	/**
	 * Returns whether the repository exposes a find one method.
	 *
	 * @return
	 */
	boolean hasFindOneMethod();

	/**
	 * Returns the delete method of the repository. Will prefer a delete-by-entity method over a delete-by-id method.
	 *
	 * @return the delete method of the repository or {@link Optional#empty()} if not available.
	 * @see #hasDelete()
	 */
	Optional<Method> getDeleteMethod();

	/**
	 * Returns whether the repository exposes a delete method.
	 *
	 * @return
	 */
	boolean hasDelete();
}
