/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository;

/**
 * Interface to abstract value objects that represent the deleted state of an entity. SPI interface that allows
 * implementing a deletion strategy over muliple levels, e.g. active, trashed, eventually soft deleted.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public interface DeleteStates<T> {

	/**
	 * Returns the value that represents the active state.
	 * 
	 * @return
	 */
	T activeValue();

	/**
	 * Return the next deleted value. This represents a state change to the next more deleted state, e.g. from active to
	 * trashed, from trashed to eventually deleted etc.
	 * 
	 * @return
	 */
	T delete();

	/**
	 * Return the value the restored object shall carry. This represents a state change to the next less deleted state,
	 * e.g. from eventually deleted to trashed, from trashed to active etc.
	 * 
	 * @return
	 */
	T restore();
}
