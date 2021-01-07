/*
 * Copyright 2011-2021 the original author or authors.
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

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

/**
 * Base class for implementations of {@link EntityInformation}. Considers an entity to be new whenever
 * {@link #getId(Object)} returns {@literal null}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Mark Paluch
 */
public abstract class AbstractEntityInformation<T, ID> implements EntityInformation<T, ID> {

	private final Class<T> domainClass;

	public AbstractEntityInformation(Class<T> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null");

		this.domainClass = domainClass;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#isNew(java.lang.Object)
	 */
	public boolean isNew(T entity) {

		ID id = getId(entity);
		Class<ID> idType = getIdType();

		if (!idType.isPrimitive()) {
			return id == null;
		}

		if (id instanceof Number) {
			return ((Number) id).longValue() == 0L;
		}

		throw new IllegalArgumentException(String.format("Unsupported primitive id type %s!", idType));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
	 */
	public Class<T> getJavaType() {
		return this.domainClass;
	}
}
