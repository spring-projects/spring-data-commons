/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.repository.support;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * A special {@link RepositoryInvoker} that shortcuts invocations to methods on {@link PagingAndSortingRepository} to
 * avoid reflection overhead introduced by the superclass.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 1.10
 */
class PagingAndSortingRepositoryInvoker extends CrudRepositoryInvoker {

	private final PagingAndSortingRepository<Object, Object> repository;
	private final boolean customFindAll;

	/**
	 * Creates a new {@link PagingAndSortingRepositoryInvoker} using the given repository, {@link RepositoryMetadata} and
	 * {@link ConversionService}.
	 *
	 * @param repository must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public PagingAndSortingRepositoryInvoker(PagingAndSortingRepository<Object, Object> repository,
			RepositoryMetadata metadata, ConversionService conversionService) {

		super((CrudRepository<Object, Object>) repository, metadata, conversionService);

		var crudMethods = metadata.getCrudMethods();

		this.repository = repository;
		this.customFindAll = isRedeclaredMethod(crudMethods.getFindAllMethod());
	}

	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return customFindAll ? invokeFindAllReflectively(sort) : repository.findAll(sort);
	}

	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return customFindAll ? invokeFindAllReflectively(pageable) : repository.findAll(pageable);
	}

	private static boolean isRedeclaredMethod(Optional<Method> method) {
		return method.map(it -> !it.getDeclaringClass().equals(PagingAndSortingRepository.class)).orElse(false);
	}
}
