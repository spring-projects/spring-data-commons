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

	Object put(Serializable id, Object item);

	boolean contains(Serializable id, Class<?> type);

	<T> T get(Serializable id, Class<T> type);

	<T> T delete(Serializable id, Class<T> type);

	<T> Collection<T> getAllOf(Class<T> type);

	void deleteAllOf(Class<?> type);

}
