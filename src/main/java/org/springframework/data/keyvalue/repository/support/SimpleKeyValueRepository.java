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
package org.springframework.data.keyvalue.repository.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @since 1.10
 * @param <T>
 * @param <ID>
 */
public class SimpleKeyValueRepository<T, ID extends Serializable> implements KeyValueRepository<T, ID> {

	private final KeyValueOperations operations;
	private final EntityInformation<T, ID> entityInformation;

	/**
	 * Creates a new {@link SimpleKeyValueRepository} for the given {@link EntityInformation} and
	 * {@link KeyValueOperations}.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 */
	public SimpleKeyValueRepository(EntityInformation<T, ID> metadata, KeyValueOperations operations) {

		Assert.notNull(metadata, "EntityInformation must not be null!");
		Assert.notNull(operations, "KeyValueOperations must not be null!");

		this.entityInformation = metadata;
		this.operations = operations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Iterable<T> findAll(Sort sort) {
		return operations.findAll(sort, entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Page<T> findAll(Pageable pageable) {

		if (pageable == null) {
			List<T> result = findAll();
			return new PageImpl<T>(result, null, result.size());
		}

		List<T> content = null;

		content = operations.findInRange(pageable.getOffset(), pageable.getPageSize(), pageable.getSort(),
				entityInformation.getJavaType());

		return new PageImpl<T>(content, pageable, this.operations.count(entityInformation.getJavaType()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#save(java.lang.Object)
	 */
	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be null!");

		if (entityInformation.isNew(entity)) {
			operations.insert(entity);
		} else {
			operations.update(entityInformation.getId(entity), entity);
		}
		return entity;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#save(java.lang.Iterable)
	 */
	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {

		for (S entity : entities) {
			save(entity);
		}

		return entities;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findOne(java.io.Serializable)
	 */
	@Override
	public T findOne(ID id) {
		return operations.findById(id, entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#exists(java.io.Serializable)
	 */
	@Override
	public boolean exists(ID id) {
		return findOne(id) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll()
	 */
	@Override
	public List<T> findAll() {
		return operations.findAll(entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#findAll(java.lang.Iterable)
	 */
	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {

		List<T> result = new ArrayList<T>();

		for (ID id : ids) {

			T candidate = findOne(id);

			if (candidate != null) {
				result.add(candidate);
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#count()
	 */
	@Override
	public long count() {
		return operations.count(entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.io.Serializable)
	 */
	@Override
	public void delete(ID id) {
		operations.delete(id, entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Object)
	 */
	@Override
	public void delete(T entity) {
		delete(entityInformation.getId(entity));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<? extends T> entities) {

		for (T entity : entities) {
			delete(entity);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#deleteAll()
	 */
	@Override
	public void deleteAll() {
		operations.delete(entityInformation.getJavaType());
	}
}
