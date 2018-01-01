/*
 * Copyright 2011-2018 the original author or authors.
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
import java.util.Collection;
import java.util.Set;

import org.springframework.data.repository.support.Repositories;

/**
 * Metadata for repository interfaces.
 *
 * @author Oliver Gierke
 */
public interface RepositoryMetadata {

	/**
	 * Returns the id class the given class is declared for.
	 *
	 * @return the id class of the entity managed by the repository.
	 */
	Class<?> getIdType();

	/**
	 * Returns the domain class the repository is declared for.
	 *
	 * @return the domain class the repository is handling.
	 */
	Class<?> getDomainType();

	/**
	 * Returns the repository interface.
	 *
	 * @return
	 */
	Class<?> getRepositoryInterface();

	/**
	 * Returns the domain class returned by the given {@link Method}. Will extract the type from {@link Collection}s and
	 * {@link org.springframework.data.domain.Page} as well.
	 *
	 * @param method
	 * @return
	 */
	Class<?> getReturnedDomainClass(Method method);

	/**
	 * Returns {@link CrudMethods} meta information for the repository.
	 *
	 * @return
	 */
	CrudMethods getCrudMethods();

	/**
	 * Returns whether the repository is a paging one.
	 *
	 * @return
	 */
	boolean isPagingRepository();

	/**
	 * Returns the set of types the repository shall be discoverable for when trying to look up a repository by domain
	 * type.
	 *
	 * @see Repositories#getRepositoryFor(Class)
	 * @return the set of types the repository shall be discoverable for when trying to look up a repository by domain
	 *         type, must not be {@literal null}.
	 * @since 1.11
	 */
	Set<Class<?>> getAlternativeDomainTypes();

	/**
	 * Returns whether the repository is a reactive one, i.e. if it uses reactive types in one of its methods.
	 *
	 * @return
	 * @since 2.0
	 */
	boolean isReactiveRepository();
}
