/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.support;

import java.io.Serializable;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.util.Assert;

/**
 * {@link CrudRepository} based {@link CrudInvoker} calling methods on {@link CrudRepository} directly.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
class CrudRepositoryInvoker<T> implements CrudInvoker<T> {

	private final CrudRepository<T, Serializable> repository;

	/**
	 * Creates a new {@link CrudRepositoryInvoker} using the given {@link CrudRepository}.
	 * 
	 * @param repository must not be {@literal null}.
	 */
	public CrudRepositoryInvoker(CrudRepository<T, Serializable> repository) {

		Assert.notNull(repository, "Repository must not be null!");
		this.repository = repository;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.CrudInvoker#findOne(java.io.Serializable)
	 */
	@Override
	public T invokeFindOne(Serializable id) {
		return repository.findOne(id);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.repository.core.CrudInvoker#save(java.lang.Object)
	 */
	@Override
	public T invokeSave(T object) {
		return repository.save(object);
	}
}
