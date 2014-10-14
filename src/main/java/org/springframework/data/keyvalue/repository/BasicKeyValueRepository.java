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
package org.springframework.data.keyvalue.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 * @param <ID>
 */
public class BasicKeyValueRepository<T, ID extends Serializable> implements KeyValueRepository<T, ID> {

	private final KeyValueOperations operations;
	private final EntityInformation<T, ID> entityInformation;

	public BasicKeyValueRepository(EntityInformation<T, ID> metadata, KeyValueOperations operations) {

		Assert.notNull(metadata, "Cannot initialize repository for 'null' metadata");
		Assert.notNull(operations, "Cannot initialize repository for 'null' operations");

		this.entityInformation = metadata;
		this.operations = operations;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Sort)
	 */
	@Override
	public Iterable<T> findAll(Sort sort) {
		return operations.findAllOf(sort, entityInformation.getJavaType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.PagingAndSortingRepository#findAll(org.springframework.data.domain.Pageable)
	 */
	@Override
	public Page<T> findAll(Pageable pageable) {

		List<T> content = null;
		if (pageable.getSort() != null) {
			content = operations.findInRange(pageable.getOffset(), pageable.getPageSize(), pageable.getSort(),
					entityInformation.getJavaType());
		} else {
			content = operations.findInRange(pageable.getOffset(), pageable.getPageSize(), entityInformation.getJavaType());
		}

		return new PageImpl<T>(content, pageable, this.operations.count(entityInformation.getJavaType()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.CrudRepository#save(java.lang.Object)
	 */
	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be 'null' for save.");

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
	public Iterable<T> findAll() {
		return operations.findAllOf(entityInformation.getJavaType());
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
