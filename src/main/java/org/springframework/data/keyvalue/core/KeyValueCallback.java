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
package org.springframework.data.keyvalue.core;

/**
 * Generic callback interface for code that operates on a {@link KeyValueAdapter}. This is particularly useful for
 * delegating code that needs to work closely on the underlying key/value store implementation.
 * 
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 */
public interface KeyValueCallback<T> {

	/**
	 * Gets called by {@code KeyValueTemplate#execute(KeyValueCallback)}. Allows for returning a result object created
	 * within the callback, i.e. a domain object or a collection of domain objects.
	 * 
	 * @param adapter
	 * @return
	 */
	T doInKeyValue(KeyValueAdapter adapter);
}
