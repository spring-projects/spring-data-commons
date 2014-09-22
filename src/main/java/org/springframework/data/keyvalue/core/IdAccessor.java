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
package org.springframework.data.keyvalue.core;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Christoph Strobl
 * @since 1.10
 */
public class IdAccessor {

	private final PersistentEntity<?, ?> entity;
	private final IdGenerator idGenerator;
	private final BeanWrapper<?> wrapper;

	@SuppressWarnings("rawtypes")
	public IdAccessor(PersistentEntity<?, ? extends PersistentProperty> entity, BeanWrapper<?> wrapper) {
		this(entity, wrapper, DefaultIdGenerator.INSTANCE);
	}

	@SuppressWarnings("rawtypes")
	public IdAccessor(PersistentEntity<?, ? extends PersistentProperty> entity, BeanWrapper<?> wrapper,
			IdGenerator idGenerator) {

		Assert.notNull(entity, "PersistentEntity must not be 'null'");
		Assert.notNull(wrapper, "BeanWrapper must not be 'null'.");

		this.idGenerator = idGenerator != null ? idGenerator : DefaultIdGenerator.INSTANCE;
		this.entity = entity;
		this.wrapper = wrapper;
	}

	public <T> T getId() {

		if (!entity.hasIdProperty()) {
			throw new InvalidDataAccessApiUsageException(String.format("Cannot determine id for type %s", entity.getType()));
		}

		PersistentProperty<?> idProperty = entity.getIdProperty();
		Object value = wrapper.getProperty(idProperty);

		if (value == null) {
			Serializable id = idGenerator.newIdForType(idProperty.getActualType());
			wrapper.setProperty(idProperty, id);
			return (T) id;
		}
		return (T) value;
	}

	/**
	 * @author Christoph Strobl
	 */
	static enum DefaultIdGenerator implements IdGenerator {

		INSTANCE;

		@Override
		public Serializable newIdForType(Class<?> idType) {

			if (ClassUtils.isAssignable(String.class, idType)) {
				return UUID.randomUUID().toString();
			} else if (ClassUtils.isAssignable(Integer.class, idType)) {
				try {
					return SecureRandom.getInstance("NativePRNGBlocking");
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("Could not create SecureRandom instance.", e);
				}
			} else if (ClassUtils.isAssignable(Long.class, idType)) {
				try {
					return SecureRandom.getInstance("NativePRNGBlocking").nextLong();
				} catch (NoSuchAlgorithmException e) {
					throw new InvalidDataAccessApiUsageException("Could not create SecureRandom instance.", e);
				}
			}

			throw new InvalidDataAccessApiUsageException("Non gereratable id type....");
		}

	}

}
