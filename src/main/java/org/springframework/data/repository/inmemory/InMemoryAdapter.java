/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory;

import java.io.Serializable;
import java.util.Collection;

/**
 * Adapter interface to connect to in memory store.
 * 
 * @author Christoph Strobl
 */
public interface InMemoryAdapter {

	/**
	 * Add object with given id to collection.
	 * 
	 * @param id must not be {@literal null}.
	 * @param collection must not be {@literal null}.
	 * @return the item previously associated with the id.
	 */
	Object put(Serializable id, Object item, Serializable collection);

	/**
	 * Check if a object with given id exists in collection.
	 * 
	 * @param id must not be {@literal null}.
	 * @param collection must not be {@literal null}.
	 * @return true if item of type with id exists.
	 */
	boolean contains(Serializable id, Serializable collection);

	/**
	 * Get the object with given id from collection.
	 * 
	 * @param id must not be {@literal null}.
	 * @param collection must not be {@literal null}.
	 * @return {@literal null} in case no matching item exists.
	 */
	Object get(Serializable id, Serializable collection);

	/**
	 * Delete and return the obect with given type and id.
	 * 
	 * @param id must not be {@literal null}.
	 * @param collection must not be {@literal null}.
	 * @return null if object could not be found
	 */
	Object delete(Serializable id, Serializable collection);

	/**
	 * Get all elements for given collection.
	 * 
	 * @param collection must not be {@literal null}.
	 * @return empty colleciton if nothing found.
	 */
	Collection<?> getAllOf(Serializable collection);

	/**
	 * Remove all objects of given type.
	 * 
	 * @param type must not be {@literal null}.
	 */
	void deleteAllOf(Serializable collection);

	/**
	 * Removes all objects.
	 */
	void clear();
}
