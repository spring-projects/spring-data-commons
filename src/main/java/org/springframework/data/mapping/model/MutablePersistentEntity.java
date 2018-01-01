/*
 * Copyright 2011-2018 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * Interface capturing mutator methods for {@link PersistentEntity}s.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface MutablePersistentEntity<T, P extends PersistentProperty<P>> extends PersistentEntity<T, P> {

	/**
	 * Adds a {@link PersistentProperty} to the entity.
	 *
	 * @param property
	 */
	void addPersistentProperty(P property);

	/**
	 * Adds an {@link Association} to the entity.
	 *
	 * @param association
	 */
	void addAssociation(Association<P> association);

	/**
	 * Callback method to trigger validation of the {@link PersistentEntity}. As {@link MutablePersistentEntity} is not
	 * immutable there might be some verification steps necessary after the object has reached is final state.
	 *
	 * @throws MappingException in case the entity is invalid
	 */
	void verify() throws MappingException;

	/**
	 * Sets the {@link PersistentPropertyAccessorFactory} for the entity. A {@link PersistentPropertyAccessorFactory}
	 * creates {@link PersistentPropertyAccessor}s for instances of this entity.
	 *
	 * @param factory must not be {@literal null}.
	 */
	void setPersistentPropertyAccessorFactory(PersistentPropertyAccessorFactory factory);
}
