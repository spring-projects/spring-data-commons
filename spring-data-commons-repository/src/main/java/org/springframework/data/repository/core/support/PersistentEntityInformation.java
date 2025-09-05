/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.core.EntityInformation;

/**
 * {@link EntityInformation} implementation that uses a {@link PersistentEntity} to obtain id type information and uses
 * a {@link org.springframework.data.mapping.IdentifierAccessor} to access the property value if requested.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class PersistentEntityInformation<T, ID> implements EntityInformation<T, ID> {

	private final PersistentEntity<T, ? extends PersistentProperty<?>> persistentEntity;

	public PersistentEntityInformation(PersistentEntity<T, ? extends PersistentProperty<?>> persistentEntity) {
		this.persistentEntity = persistentEntity;
	}

	@Override
	public boolean isNew(T entity) {
		return persistentEntity.isNew(entity);
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable ID getId(T entity) {
		return (ID) persistentEntity.getIdentifierAccessor(entity).getIdentifier();
	}

	@Override
	public Class<T> getJavaType() {
		return persistentEntity.getType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) persistentEntity.getRequiredIdProperty().getType();
	}
}
