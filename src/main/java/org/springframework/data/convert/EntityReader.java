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
package org.springframework.data.convert;

/**
 * Interface to read object from store specific sources.
 *
 * @author Oliver Gierke
 */
public interface EntityReader<T, S> {

	/**
	 * Reads the given source into the given type.
	 *
	 * @param type they type to convert the given source to.
	 * @param source the source to create an object of the given type from.
	 * @return
	 */
	<R extends T> R read(Class<R> type, S source);
}
