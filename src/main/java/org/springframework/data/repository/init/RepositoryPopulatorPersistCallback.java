/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.repository.init;

import org.springframework.data.repository.support.Repositories;

/**
 * Provide callback functionality when an entity is persisted in {@link RepositoryPopulator}
 *
 * @author Rocco Lagrotteria
 * @since 2.7
 */
@FunctionalInterface
public interface RepositoryPopulatorPersistCallback {

	  /**
	   * Possibly called by {@link RepositoryPopulator} after an entity is persisted
	   * 
	   * 
	   * @param repositories The current wrapped repository instances
	   * @param entity The entity passed to a repository to being saved
	   * @param outcome The result of the save operation
	   */
		void afterPersist(Repositories repositories, Object entity, Object outcome);

}
