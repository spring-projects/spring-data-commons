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
package org.springframework.data.repository.inmemory;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Implementation of {@link InMemoryRepository}
 * 
 * @author Christoph Strobl
 */
public class BasicInMemoryRepository<T, ID extends Serializable> implements InMemoryRepository<T, ID> {

	private final InMemoryOperations operations;
	private final ReflectionEntityInformation<T, ID> entityInformation;

	public BasicInMemoryRepository(EntityInformation<T, ID> metadata, InMemoryOperations operations) {

		Assert.notNull(metadata, "Cannot initialize repository for 'null' metadata");
		Assert.notNull(metadata, "Cannot initialize repository for 'null' operations");

		this.entityInformation = metadata instanceof ReflectionEntityInformation ? (ReflectionEntityInformation<T, ID>) metadata
				: new ReflectionEntityInformation<T, ID>(metadata.getJavaType());
		this.operations = operations;
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		throw new UnsupportedOperationException("Sort currently not supported.");
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		if (pageable.getSort() != null) {
			throw new UnsupportedOperationException("sort currently not supported.");
		}
		return new PageImpl<T>(operations.read(pageable.getOffset(), pageable.getPageSize(),
				entityInformation.getJavaType()), pageable, this.operations.count(entityInformation.getJavaType()));
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be 'null' for save.");

		if (entityInformation.isNew(entity)) {

			Serializable id = null;
			if (ClassUtils.isAssignable(String.class, entityInformation.getIdType())) {
				id = UUID.randomUUID().toString();
			} else if (ClassUtils.isAssignable(Integer.class, entityInformation.getIdType())) {
				try {
					id = SecureRandom.getInstanceStrong().nextInt();
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("argh....");
				}
			} else if (ClassUtils.isAssignable(Long.class, entityInformation.getIdType())) {
				try {
					id = SecureRandom.getInstanceStrong().nextLong();
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("argh....");
				}
			}

			ReflectionUtils.setField(entityInformation.getIdField(), entity, id);
			operations.create(id, entity);
		} else {
			operations.update(entityInformation.getId(entity), entity);
		}
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {

		for (S entity : entities) {
			save(entity);
		}
		return entities;
	}

	@Override
	public T findOne(ID id) {
		return operations.read(id, entityInformation.getJavaType());
	}

	@Override
	public boolean exists(ID id) {
		return findOne(id) != null;
	}

	@Override
	public Iterable<T> findAll() {
		return operations.read(entityInformation.getJavaType());
	}

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

	@Override
	public long count() {
		return operations.count(entityInformation.getJavaType());
	}

	@Override
	public void delete(ID id) {
		operations.delete(id, entityInformation.getJavaType());
	}

	@Override
	public void delete(T entity) {
		delete(entityInformation.getId(entity));
	}

	@Override
	public void delete(Iterable<? extends T> entities) {

		for (T entity : entities) {
			delete(entity);
		}
	}

	@Override
	public void deleteAll() {
		operations.delete(entityInformation.getJavaType());
	}

}
