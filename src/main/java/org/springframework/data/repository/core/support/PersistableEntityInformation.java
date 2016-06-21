/*
 * Copyright 2008-201 the original author or authors.
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
import java.util.Optional;

import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.EntityMetadata;

/**
 * Implementation of {@link EntityMetadata} that assumes the entity handled implements {@link Persistable} and uses
 * {@link Persistable#isNew()} for the {@link #isNew(Object)} check.
 * 
 * @author Oliver Gierke
 */
public class PersistableEntityInformation<T extends Persistable<ID>, ID extends Serializable>
		extends AbstractEntityInformation<T, ID> {

	private Class<ID> idClass;

	/**
	 * Creates a new {@link PersistableEntityInformation}.
	 * 
	 * @param domainClass
	 */
	@SuppressWarnings("unchecked")
	public PersistableEntityInformation(Class<T> domainClass) {

		super(domainClass);
		this.idClass = (Class<ID>) GenericTypeResolver.resolveTypeArgument(domainClass, Persistable.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.AbstractEntityInformation#isNew(java.lang.Object)
	 */
	@Override
	public boolean isNew(T entity) {
		return entity.isNew();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@Override
	public Optional<ID> getId(T entity) {
		return Optional.ofNullable(entity.getId());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@Override
	public Class<ID> getIdType() {
		return this.idClass;
	}
}
