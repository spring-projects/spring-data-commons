/*
 * Copyright 2026-present the original author or authors.
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
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * {@link RepositoryInvoker} to shortcut execution of CRUD methods into direct calls on a
 * {@link ReactiveCrudRepository}. This invoker blocks on reactive types to provide synchronous
 * execution semantics required by the repository populator infrastructure.
 *
 * @author Gichan Im
 * @since 4.1
 * @see ReactiveCrudRepository
 */
class ReactiveCrudRepositoryInvoker extends ReflectionRepositoryInvoker {

	private final ReactiveCrudRepository<Object, Object> repository;

	private final boolean customSaveMethod;
	private final boolean customFindOneMethod;
	private final boolean customFindAllMethod;
	private final boolean customDeleteMethod;

	/**
	 * Creates a new {@link ReactiveCrudRepositoryInvoker} for the given {@link ReactiveCrudRepository},
	 * {@link RepositoryMetadata} and {@link ConversionService}.
	 *
	 * @param repository must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public ReactiveCrudRepositoryInvoker(ReactiveCrudRepository<Object, Object> repository, RepositoryMetadata metadata,
			ConversionService conversionService) {

		super(repository, metadata, conversionService);

		CrudMethods crudMethods = metadata.getCrudMethods();

		this.customSaveMethod = isRedeclaredMethod(crudMethods.getSaveMethod());
		this.customFindOneMethod = isRedeclaredMethod(crudMethods.getFindOneMethod());
		this.customDeleteMethod = isRedeclaredMethod(crudMethods.getDeleteMethod());
		this.customFindAllMethod = isRedeclaredMethod(crudMethods.getFindAllMethod());
		this.repository = repository;
	}

	@Override
	public Iterable<Object> invokeFindAll(Sort sort) {
		return customFindAllMethod ? super.invokeFindAll(sort) : repository.findAll().collectList().block();
	}

	@Override
	public Iterable<Object> invokeFindAll(Pageable pageable) {
		return customFindAllMethod ? super.invokeFindAll(pageable) : repository.findAll().collectList().block();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Optional<T> invokeFindById(Object id) {
		return customFindOneMethod ? super.invokeFindById(id)
				: (Optional<T>) repository.findById(convertId(id)).blockOptional();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T invokeSave(T entity) {
		return customSaveMethod ? super.invokeSave(entity) : (T) repository.save(entity).block();
	}

	@Override
	public void invokeDeleteById(Object id) {

		if (customDeleteMethod) {
			super.invokeDeleteById(id);
		} else {
			repository.deleteById(convertId(id)).block();
		}
	}

	private static boolean isRedeclaredMethod(Optional<Method> method) {
		return method.map(it -> !it.getDeclaringClass().equals(ReactiveCrudRepository.class)).orElse(false);
	}
}
