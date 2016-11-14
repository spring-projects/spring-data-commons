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
package org.springframework.data.repository.core.support;

import java.io.Serializable;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.EntityInformation;

/**
 * {@link EntityInformation} implementation that uses a {@link PersistentEntity} to obtain id type information and uses
 * a {@link org.springframework.data.mapping.IdentifierAccessor} to access the property value if requested.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("unchecked")
public class PersistentEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

	private final PersistentEntity<T, ?> persistentEntity;

	/**
	 * Creates a new {@link PersistableEntityInformation} for the given {@link PersistentEntity}.
	 * 
	 * @param entity must not be {@literal null}.
	 */
	public PersistentEntityInformation(PersistentEntity<T, ?> entity) {

		super(entity.getType());
		this.persistentEntity = entity;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@Override
	public ID getId(T entity) {
		return (ID) persistentEntity.getIdentifierAccessor(entity).getIdentifier().orElse(null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@Override
	public Class<ID> getIdType() {
		return (Class<ID>) persistentEntity.getIdProperty().map(it -> it.getType()).orElse(null);
	}
}
