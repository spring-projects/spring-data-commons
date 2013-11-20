/*
 * Copyright 2011-2013 the original author or authors.
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

import java.io.Serializable;

import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.Assert;

/**
 * {@link RepositoryMetadata} implementation inspecting the given repository interface for a
 * {@link RepositoryDefinition} annotation.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AnnotationRepositoryMetadata extends AbstractRepositoryMetadata {

	private static final String NO_ANNOTATION_FOUND = String.format("Interface must be annotated with @%s!",
			RepositoryDefinition.class.getName());

	private final Class<? extends Serializable> idType;
	private final Class<?> domainType;

	/**
	 * Creates a new {@link AnnotationRepositoryMetadata} instance looking up repository types from a
	 * {@link RepositoryDefinition} annotation.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public AnnotationRepositoryMetadata(Class<?> repositoryInterface) {

		super(repositoryInterface);
		Assert.isTrue(repositoryInterface.isAnnotationPresent(RepositoryDefinition.class), NO_ANNOTATION_FOUND);

		this.idType = resolveIdType(repositoryInterface);
		this.domainType = resolveDomainType(repositoryInterface);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getIdType()
	 */
	@Override
	public Class<? extends Serializable> getIdType() {
		return this.idType;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.RepositoryMetadata#getDomainType()
	 */
	@Override
	public Class<?> getDomainType() {
		return this.domainType;
	}

	/**
	 * @param repositoryInterface must not be {@literal null}.
	 * @return the resolved domain type, never {@literal null}.
	 */
	private Class<? extends Serializable> resolveIdType(Class<?> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null!");

		RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);
		Assert.isTrue(annotation != null && annotation.idClass() != null,
				String.format("Could not resolve id type of %s!", repositoryInterface));

		return annotation.idClass();
	}

	/**
	 * @param repositoryInterface must not be {@literal null}.
	 * @return the resolved domain type, never {@literal null}.
	 */
	private Class<?> resolveDomainType(Class<?> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null!");

		RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);
		Assert.isTrue(annotation != null && annotation.domainClass() != null,
				String.format("Could not resolve domain type of %s!", repositoryInterface));

		return annotation.domainClass();
	}
}
