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
package org.springframework.data.sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.util.Assert;

/**
 * {@link PersistenceCallback} implementation to be backed by a {@link RepositoryInvoker}.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
class RepositoryPersistenceCallback<T> implements PersistenceCallback<T> {

	private final RepositoryInvoker repo;
	private final RepositoryMetadata metadata;
	private final PersistentEntities persistentEntities;

	/**
	 * Creates a new {@link RepositoryPersistenceCallback} for the given {@link RepositoryInvoker},
	 * {@link RepositoryMetadata} and {@link ConversionService}.
	 * 
	 * @param invoker must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param persistentEntities must not be {@literal null}.
	 */
	public RepositoryPersistenceCallback(RepositoryInvoker invoker, RepositoryMetadata metadata,
			PersistentEntities persistentEntities) {

		Assert.notNull(invoker, "RepositoryInvoker must not be null!");
		Assert.notNull(metadata, "RepositoryMetadata must not be null!");
		Assert.notNull(persistentEntities, "PersistentEntities must not be null!");

		this.repo = invoker;
		this.metadata = metadata;
		this.persistentEntities = persistentEntities;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#findAll()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<T> findAll() {

		List<T> result = new ArrayList<T>();

		for (Object element : repo.invokeFindAll((Pageable) null)) {
			result.add((T) element);
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#findOne(java.lang.String)
	 */
	@Override
	public T findOne(String id) {
		return repo.invokeFindOne(id);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#persistChange(java.lang.Object)
	 */
	@Override
	public void persistChange(T itemToSave) {
		repo.invokeSave(itemToSave);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#persistChanges(java.util.List, java.util.List)
	 */
	@Override
	public void persistChanges(List<T> itemsToSave, List<T> itemsToDelete) {

		for (T item : itemsToSave) {
			repo.invokeSave(item);
		}

		for (T item : itemsToDelete) {

			PersistentEntity<?, ?> entity = persistentEntities.getPersistentEntity(item.getClass());
			IdentifierAccessor accessor = entity.getIdentifierAccessor(item);

			repo.invokeDelete((Serializable) accessor.getIdentifier());
		}

		throw new UnsupportedOperationException("Delete not yet supported!");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#getEntityType()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getEntityType() {
		return (Class<T>) metadata.getDomainType();
	}
}
