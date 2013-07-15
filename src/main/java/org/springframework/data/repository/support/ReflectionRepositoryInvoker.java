/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.support;

import static java.lang.String.*;

import java.io.Serializable;

import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link CrudInvoker} that uses reflection to invoke repository methods based on the {@link CrudMethods} meta
 * information.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
class ReflectionRepositoryInvoker<T> implements CrudInvoker<T> {

	private final Object repository;
	private final CrudMethods methods;

	/**
	 * Creates a new {@link ReflectionRepositoryInvoker} using the given repository and {@link CrudMethods}.
	 * 
	 * @param repository must not be {@literal null}.
	 * @param methods must not be {@literal null}.
	 */
	public ReflectionRepositoryInvoker(Object repository, CrudMethods methods) {

		Assert.notNull(repository, "Repository must not be null!");
		Assert.notNull(methods, "CrudMethods must not be null!");

		Class<?> type = repository.getClass();
		Assert.isTrue(methods.hasFindOneMethod(), format("Repository %s does not expose a findOne(…) method!", type));
		Assert.isTrue(methods.hasSaveMethod(), format("Repository %s does not expose a save(…) method!", type));

		this.repository = repository;
		this.methods = methods;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.CrudInvoker#save(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T invokeSave(T object) {
		return (T) ReflectionUtils.invokeMethod(methods.getSaveMethod(), repository, object);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.CrudInvoker#findOne(java.io.Serializable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public T invokeFindOne(Serializable id) {
		return (T) ReflectionUtils.invokeMethod(methods.getFindOneMethod(), id);
	}
}
