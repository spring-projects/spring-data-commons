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

import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReflectionEntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.ClassUtils;

/**
 * Abstract {@link RepositoryFactorySupport} implementation of {@link InMemoryRepository} holding basic type
 * definitions.
 * 
 * @author Christoph Strobl
 * @param <T>
 * @param <ID>
 */
public abstract class InMemoryRepositoryFactory<T, ID extends Serializable> extends RepositoryFactorySupport {

	@Override
	public <T, ID extends Serializable> ReflectionEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return new ReflectionEntityInformation<T, ID>(domainClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object getTargetRepository(RepositoryMetadata metadata) {

		EntityInformation<T, ID> entityInformation = this.<T, ID> getEntityInformation((Class<T>) metadata.getDomainType());
		if (ClassUtils.isAssignable(QueryDslPredicateExecutor.class, metadata.getRepositoryInterface())) {
			return new QueryDslInMemoryRepository<T, ID>(entityInformation, getInMemoryOperations());
		}
		return new BasicInMemoryRepository<T, ID>(entityInformation, getInMemoryOperations());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return BasicInMemoryRepository.class;
	}

	/**
	 * @return store specific implementation
	 */
	protected abstract InMemoryOperations getInMemoryOperations();
}
