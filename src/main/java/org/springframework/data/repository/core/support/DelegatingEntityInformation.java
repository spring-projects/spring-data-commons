/*
 * Copyright 2012-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.Nullable;

/**
 * Useful base class to implement custom {@link EntityInformation}s and delegate execution of standard methods from
 * {@link EntityInformation} to a special implementation.
 *
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DelegatingEntityInformation<T, ID> implements EntityInformation<T, ID> {

	private final @NonNull EntityInformation<T, ID> delegate;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityMetadata#getJavaType()
	 */
	@Override
	public Class<T> getJavaType() {
		return delegate.getJavaType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#isNew(java.lang.Object)
	 */
	@Override
	public boolean isNew(T entity) {
		return delegate.isNew(entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@Nullable
	@Override
	public ID getId(T entity) {
		return delegate.getId(entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@Override
	public Class<ID> getIdType() {
		return delegate.getIdType();
	}
}
