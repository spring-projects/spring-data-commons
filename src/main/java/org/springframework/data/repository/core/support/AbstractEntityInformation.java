/*
 * Copyright 2011-2013 the original author or authors.
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

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

/**
 * Base class for implementations of {@link EntityInformation}. Considers an entity to be new whenever
 * {@link #getId(Object)} returns {@literal null}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public abstract class AbstractEntityInformation<T, ID extends Serializable> implements EntityInformation<T, ID> {

	private final Class<T> domainClass;

	/**
	 * Creates a new {@link AbstractEntityInformation} from the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 */
	public AbstractEntityInformation(Class<T> domainClass) {

		Assert.notNull(domainClass);
		this.domainClass = domainClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#isNew(java.lang.Object)
	 */
	public boolean isNew(T entity) {

		Optional<ID> id = getId(entity);
		Class<ID> idType = getIdType();

		if (!idType.isPrimitive()) {
			return !id.isPresent();
		}

		return id.map(it -> {

			if (it instanceof Number) {
				return ((Number) it).longValue() == 0L;
			}

			return null;

		}).orElseThrow(() -> new IllegalArgumentException(String.format("Unsupported primitive id type %s!", idType)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
	 */
	public Class<T> getJavaType() {
		return this.domainClass;
	}
}
