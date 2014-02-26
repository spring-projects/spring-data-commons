/*
 * Copyright 2011-2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryMetadata} implementations.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public abstract class AbstractRepositoryMetadata implements RepositoryMetadata {

	private final TypeInformation<?> typeInformation;
	private final Class<?> repositoryInterface;
	private CrudMethods crudMethods;

	/**
	 * Creates a new {@link AbstractRepositoryMetadata}.
	 * 
	 * @param repositoryInterface must not be {@literal null} and must be an interface.
	 */
	public AbstractRepositoryMetadata(Class<?> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Given type must not be null!");
		Assert.isTrue(repositoryInterface.isInterface(), "Given type must be an interface!");

		this.repositoryInterface = repositoryInterface;
		this.typeInformation = ClassTypeInformation.from(repositoryInterface);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnedDomainClass(java.lang.reflect.Method)
	 */
	public Class<?> getReturnedDomainClass(Method method) {

		TypeInformation<?> returnTypeInfo = typeInformation.getReturnType(method);
		Class<?> rawType = returnTypeInfo.getType();

		return Iterable.class.isAssignableFrom(rawType) ? returnTypeInfo.getComponentType().getType() : rawType;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getRepositoryInterface()
	 */
	public Class<?> getRepositoryInterface() {
		return this.repositoryInterface;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getCrudMethods()
	 */
	@Override
	public CrudMethods getCrudMethods() {

		if (this.crudMethods == null) {
			this.crudMethods = new DefaultCrudMethods(this);
		}

		return this.crudMethods;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#isPagingRepository()
	 */
	@Override
	public boolean isPagingRepository() {

		Method findAllMethod = getCrudMethods().getFindAllMethod();

		if (findAllMethod == null) {
			return false;
		}

		return Arrays.asList(findAllMethod.getParameterTypes()).contains(Pageable.class);
	}
}
