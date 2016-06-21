/*
 * Copyright 2011 the original author or authors.
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

import org.springframework.data.mapping.Alias;

/**
 * Interface to abstract implementations of how to access a type alias from a given source or sink.
 * 
 * @author Oliver Gierke
 */
public interface TypeAliasAccessor<S> {

	/**
	 * Reads the type alias to be used from the given source.
	 * 
	 * @param source
	 * @return can be {@literal null} in case no alias was found.
	 */
	Alias readAliasFrom(S source);

	/**
	 * Writes the given type alias to the given sink.
	 * 
	 * @param sink
	 * @param alias
	 */
	void writeTypeTo(S sink, Object alias);
}
