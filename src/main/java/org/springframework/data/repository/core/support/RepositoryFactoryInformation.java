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
package org.springframework.data.repository.core.support;

import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Interface for components that can provide meta-information about a repository factory, the backing
 * {@link EntityInformation} and {@link RepositoryInformation} as well as the {@link QueryMethod}s exposed by the
 * repository.
 *
 * @author Oliver Gierke
 */
public interface RepositoryFactoryInformation<T, ID> {

	/**
	 * Returns {@link EntityInformation} the repository factory is using.
	 *
	 * @return
	 */
	EntityInformation<T, ID> getEntityInformation();

	/**
	 * Returns the {@link RepositoryInformation} to determine meta-information about the repository being used.
	 *
	 * @return
	 */
	RepositoryInformation getRepositoryInformation();

	/**
	 * Returns the {@link PersistentEntity} managed by the underlying repository. Can be {@literal null} in case the
	 * underlying persistence mechanism does not expose a {@link MappingContext}.
	 *
	 * @return
	 */
	PersistentEntity<?, ?> getPersistentEntity();

	/**
	 * Returns all {@link QueryMethod}s declared for that repository.
	 *
	 * @return
	 */
	List<QueryMethod> getQueryMethods();
}
