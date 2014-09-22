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

import org.springframework.expression.Expression;

/**
 * @author Christoph Strobl
 */
public interface InMemoryOperations {

	void create(Serializable id, Object objectToInsert);

	<T> List<T> read(Class<T> type);

	<T> T read(Serializable id, Class<T> type);

	<T> T execute(MapCallback<T> action);

	<T> List<T> read(Expression filter, Class<T> type);

	<T> List<T> read(int offset, int rows, Class<T> type);

	<T> List<T> read(Expression filter, int offset, int rows, Class<T> type);

	void update(Serializable id, Object objectToUpdate);

	void delete(Class<?> type);

	<T> T delete(Serializable id, Class<T> type);

	long count(Class<?> type);

	long count(Expression filter, Class<?> type);

}
