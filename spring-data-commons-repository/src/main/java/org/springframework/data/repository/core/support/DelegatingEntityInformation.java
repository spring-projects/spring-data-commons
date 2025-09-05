/*
 * Copyright 2012-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.core.EntityInformation;

/**
 * Useful base class to implement custom {@link EntityInformation}s and delegate execution of standard methods from
 * {@link EntityInformation} to a special implementation.
 *
 * @author Oliver Gierke
 */
public class DelegatingEntityInformation<T, ID> implements EntityInformation<T, ID> {

	private final EntityInformation<T, ID> delegate;

	public DelegatingEntityInformation(EntityInformation<T, ID> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Class<T> getJavaType() {
		return delegate.getJavaType();
	}

	@Override
	public boolean isNew(T entity) {
		return delegate.isNew(entity);
	}

	@Override
	public @Nullable ID getId(T entity) {
		return delegate.getId(entity);
	}

	@Override
	public Class<ID> getIdType() {
		return delegate.getIdType();
	}
}
