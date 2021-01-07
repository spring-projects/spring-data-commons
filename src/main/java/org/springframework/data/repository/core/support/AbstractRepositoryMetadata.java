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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.core.KotlinDetector;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.util.QueryExecutionConverters;
import org.springframework.data.repository.util.ReactiveWrapperConverters;
import org.springframework.data.repository.util.ReactiveWrappers;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryMetadata} implementations.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Mark Paluch
 */
public abstract class AbstractRepositoryMetadata implements RepositoryMetadata {

	private final TypeInformation<?> typeInformation;
	private final Class<?> repositoryInterface;
	private final Lazy<CrudMethods> crudMethods;

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
		this.crudMethods = Lazy.of(() -> new DefaultCrudMethods(this));
	}

	/**
	 * Creates a new {@link RepositoryMetadata} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 * @since 1.9
	 * @return
	 */
	public static RepositoryMetadata getMetadata(Class<?> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null!");

		return Repository.class.isAssignableFrom(repositoryInterface) ? new DefaultRepositoryMetadata(repositoryInterface)
				: new AnnotationRepositoryMetadata(repositoryInterface);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnType(java.lang.reflect.Method)
	 */
	@Override
	public TypeInformation<?> getReturnType(Method method) {

		TypeInformation<?> returnType = null;
		if (KotlinDetector.isKotlinType(method.getDeclaringClass()) && KotlinReflectionUtils.isSuspend(method)) {

			// the last parameter is Continuation<? super T> or Continuation<? super Flow<? super T>>
			List<TypeInformation<?>> types = typeInformation.getParameterTypes(method);
			returnType = types.get(types.size() - 1).getComponentType();
		}

		if (returnType == null) {
			returnType = typeInformation.getReturnType(method);
		}

		return returnType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getReturnedDomainClass(java.lang.reflect.Method)
	 */
	public Class<?> getReturnedDomainClass(Method method) {

		TypeInformation<?> returnType = getReturnType(method);

		return QueryExecutionConverters.unwrapWrapperTypes(ReactiveWrapperConverters.unwrapWrapperTypes(returnType))
				.getType();
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
		return this.crudMethods.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#isPagingRepository()
	 */
	@Override
	public boolean isPagingRepository() {

		return getCrudMethods().getFindAllMethod()//
				.map(it -> Arrays.asList(it.getParameterTypes()).contains(Pageable.class))//
				.orElse(false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getAlternativeDomainTypes()
	 */
	@Override
	public Set<Class<?>> getAlternativeDomainTypes() {
		return Collections.emptySet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#isReactiveRepository()
	 */
	@Override
	public boolean isReactiveRepository() {
		return ReactiveWrappers.usesReactiveType(repositoryInterface);
	}
}
