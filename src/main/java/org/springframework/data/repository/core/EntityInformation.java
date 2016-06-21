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
package org.springframework.data.repository.core;

import java.io.Serializable;
import java.util.Optional;

/**
 * Extension of {@link EntityMetadata} to add functionality to query information of entity instances.
 * 
 * @author Oliver Gierke
 */
public interface EntityInformation<T, ID extends Serializable> extends EntityMetadata<T> {

	/**
	 * Returns whether the given entity is considered to be new.
	 * 
	 * @param entity must never be {@literal null}
	 * @return
	 */
	boolean isNew(T entity);

	/**
	 * Returns the id of the given entity.
	 * 
	 * @param entity must never be {@literal null}
	 * @return
	 */
	Optional<ID> getId(T entity);

	/**
	 * Returns the type of the id of the entity.
	 * 
	 * @return
	 */
	Class<ID> getIdType();
}
