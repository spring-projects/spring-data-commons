/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.domain;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

/**
 * Interface for auditable entities. Allows storing and retrieving creation and modification information. The changing
 * instance (typically some user) is to be defined by a generics definition.
 *
 * @param <U> the auditing type. Typically some kind of user.
 * @param <ID> the type of the audited type's identifier
 * @author Oliver Gierke
 */
public interface Auditable<U, ID, T extends TemporalAccessor> extends Persistable<ID> {

	/**
	 * Returns the user who created this entity.
	 *
	 * @return the createdBy
	 */
	Optional<U> getCreatedBy();

	/**
	 * Sets the user who created this entity.
	 *
	 * @param createdBy the creating entity to set
	 */
	void setCreatedBy(U createdBy);

	/**
	 * Returns the creation date of the entity.
	 *
	 * @return the createdDate
	 */
	Optional<T> getCreatedDate();

	/**
	 * Sets the creation date of the entity.
	 *
	 * @param creationDate the creation date to set
	 */
	void setCreatedDate(T creationDate);

	/**
	 * Returns the user who modified the entity lastly.
	 *
	 * @return the lastModifiedBy
	 */
	Optional<U> getLastModifiedBy();

	/**
	 * Sets the user who modified the entity lastly.
	 *
	 * @param lastModifiedBy the last modifying entity to set
	 */
	void setLastModifiedBy(U lastModifiedBy);

	/**
	 * Returns the date of the last modification.
	 *
	 * @return the lastModifiedDate
	 */
	Optional<T> getLastModifiedDate();

	/**
	 * Sets the date of the last modification.
	 *
	 * @param lastModifiedDate the date of the last modification to set
	 */
	void setLastModifiedDate(T lastModifiedDate);
}
