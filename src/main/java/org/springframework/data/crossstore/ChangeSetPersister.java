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
package org.springframework.data.crossstore;

import org.springframework.dao.DataAccessException;

/**
 * Interface to be implemented by classes that can synchronize between data stores and ChangeSets.
 *
 * @param <K> entity key
 * @author Rod Johnson
 */
public interface ChangeSetPersister<K> {

	String ID_KEY = "_id";
	String CLASS_KEY = "_class";

	/**
	 * TODO how to tell when not found? throw exception?
	 */
	void getPersistentState(Class<? extends ChangeSetBacked> entityClass, K key, ChangeSet changeSet)
			throws DataAccessException, NotFoundException;

	/**
	 * Return id
	 *
	 * @param entity
	 * @param cs
	 * @return
	 * @throws DataAccessException
	 */
	K getPersistentId(ChangeSetBacked entity, ChangeSet cs) throws DataAccessException;

	/**
	 * Return key
	 *
	 * @param entity
	 * @param cs Key may be null if not persistent
	 * @return
	 * @throws DataAccessException
	 */
	K persistState(ChangeSetBacked entity, ChangeSet cs) throws DataAccessException;

	/**
	 * Exception thrown in alternate control flow if getPersistentState finds no entity data.
	 */
	class NotFoundException extends Exception {

		private static final long serialVersionUID = -8604207973816331140L;
	}

}
