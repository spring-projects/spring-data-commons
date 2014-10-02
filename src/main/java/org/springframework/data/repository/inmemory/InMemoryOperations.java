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
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;

/**
 * @author Christoph Strobl
 */
public interface InMemoryOperations extends DisposableBean {

	/**
	 * Add given object.
	 * 
	 * @param objectToInsert
	 * @return
	 */
	<T> T create(T objectToInsert);

	/**
	 * Add object with given id.
	 * 
	 * @param id must not be {@literal null}.
	 * @param objectToInsert must not be {@literal null}.
	 */
	void create(Serializable id, Object objectToInsert);

	/**
	 * Get all elements of given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return empty collection if no elements found.
	 */
	<T> List<T> read(Class<T> type);

	/**
	 * Get element of given type with given id.
	 * 
	 * @param id must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return null if not found.
	 */
	<T> T read(Serializable id, Class<T> type);

	/**
	 * Execute operation against underlying store.
	 * 
	 * @param action must not be {@literal null}.
	 * @return
	 */
	<T> T execute(InMemoryCallback<T> action);

	/**
	 * @param query
	 * @param type
	 * @return empty collection if no match found.
	 */
	<T> List<T> read(InMemoryQuery query, Class<T> type);

	/**
	 * @param sort
	 * @param type
	 * @return
	 */
	<T> List<T> read(Sort sort, Class<T> type);

	/**
	 * @param offset
	 * @param rows
	 * @param type
	 * @return
	 */
	<T> List<T> read(int offset, int rows, Class<T> type);

	/**
	 * @param offset
	 * @param rows
	 * @param sort
	 * @param type
	 * @return
	 */
	<T> List<T> read(int offset, int rows, Sort sort, Class<T> type);

	/**
	 * @param objectToUpdate
	 */
	void update(Object objectToUpdate);

	/**
	 * @param id must not be {@literal null}.
	 * @param objectToUpdate must not be {@literal null}.
	 */
	void update(Serializable id, Object objectToUpdate);

	/**
	 * Remove items of given type.
	 * 
	 * @param type must not be {@literal null}.
	 */
	void delete(Class<?> type);

	/**
	 * @param objectToDelete
	 * @return
	 */
	<T> T delete(T objectToDelete);

	/**
	 * Delete item of type with given id.
	 * 
	 * @param id
	 * @param type
	 * @return the deleted item or {@literal null} if no match found.
	 */
	<T> T delete(Serializable id, Class<T> type);

	/**
	 * Total number of elements with given type available.
	 * 
	 * @param type
	 * @return
	 */
	long count(Class<?> type);

	/**
	 * Total number of elements matching given query.
	 * 
	 * @param query
	 * @param type
	 * @return
	 */
	long count(InMemoryQuery query, Class<?> type);

	MappingContext<?, ?> getMappingContext();

}
