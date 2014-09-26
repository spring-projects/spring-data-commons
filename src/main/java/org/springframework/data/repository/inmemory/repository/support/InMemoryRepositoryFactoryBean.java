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
package org.springframework.data.repository.inmemory.repository.support;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.inmemory.InMemoryOperations;
import org.springframework.data.repository.inmemory.InMemoryRepository;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} to create {@link InMemoryRepository}.
 * 
 * @author Christoph Strobl
 */
public class InMemoryRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
		RepositoryFactoryBeanSupport<T, S, ID> {

	private InMemoryOperations operations;
	private Class<? extends RepositoryFactorySupport> repositoryFactoryType;
	private boolean mappingContextAvailable = false;

	public void setInMemoryOperations(InMemoryOperations operations) {
		this.operations = operations;
	}

	public void setRepositoryFactoryType(Class<? extends RepositoryFactorySupport> repositoryFactoryType) {
		this.repositoryFactoryType = repositoryFactoryType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#setMappingContext(org.springframework.data.mapping.context.MappingContext)
	 */
	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {

		super.setMappingContext(mappingContext);
		this.mappingContextAvailable = mappingContext != null;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {

		Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(repositoryFactoryType,
				ClassUtils.getUserClass(operations));

		try {
			return (RepositoryFactorySupport) (constructor != null ? constructor.newInstance(operations)
					: repositoryFactoryType.newInstance());
		} catch (InstantiationException e) {
			throw new UnsupportedOperationException("Cannot create repository factory.", e);
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException("Cannot create repository factory.", e);
		} catch (IllegalArgumentException e) {
			throw new UnsupportedOperationException("Cannot create repository factory.", e);
		} catch (InvocationTargetException e) {
			throw new UnsupportedOperationException("Cannot create repository factory.", e);
		}
	}

	@Override
	public void afterPropertiesSet() {

		if (!mappingContextAvailable) {
			super.setMappingContext(operations.getMappingContext());
		}

		super.afterPropertiesSet();
	}

}
