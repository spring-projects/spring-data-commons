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

import java.util.Collections;
import java.util.List;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.sync.diffsync.PersistenceCallback;
import org.springframework.util.Assert;

/**
 * {@link PersistenceCallback} implementation to be backed by a {@link CrudInvoker} (which currently renders the
 * implementation incomplete until DATACMNS-589 gets resolved).
 *
 * @author Oliver Gierke
 * @see DATCMNS-598
 * @see Spring Data REST's RepositoryInvoker API
 */
class RepositoryPersistenceCallback<T> implements PersistenceCallback<T> {

	private final CrudInvoker<T> repo;
	private final RepositoryMetadata metadata;
	private final ConversionService conversionService;

	/**
	 * Creates a new {@link RepositoryPersistenceCallback} for the given {@link CrudInvoker}, {@link RepositoryMetadata}
	 * and {@link ConversionService}.
	 * 
	 * @param invoker must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public RepositoryPersistenceCallback(CrudInvoker<T> invoker, RepositoryMetadata metadata,
			ConversionService conversionService) {

		Assert.notNull(invoker, "CrudInvoker must not be null!");
		Assert.notNull(metadata, "RepositoryMetadata must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.repo = invoker;
		this.metadata = metadata;
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#findAll()
	 */
	@Override
	public List<T> findAll() {
		return Collections.emptyList();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.PersistenceCallback#findOne(java.lang.String)
	 */
	@Override
	public T findOne(String id) {
		return repo.invokeFindOne(conversionService.convert(id, metadata.getIdType()));
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
