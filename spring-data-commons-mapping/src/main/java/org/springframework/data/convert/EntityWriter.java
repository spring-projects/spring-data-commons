/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.convert;

/**
 * Interface to write objects into store specific sinks.
 *
 * @param <T> the entity type the converter can handle
 * @param <S> the store specific sink the converter is able to write to
 * @author Oliver Gierke
 */
public interface EntityWriter<T, S> {

	/**
	 * Writes the given source object into a store specific sink.
	 *
	 * @param source the source to create an object of the given type from.
	 * @param sink the sink to write into.
	 */
	void write(T source, S sink);
}
