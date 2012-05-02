/*
 * Copyright 2011-2012 the original author or authors.
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
 */
public class AnnotationRepositoryMetadata extends AbstractRepositoryMetadata {

	private static final String NO_ANNOTATION_FOUND = String.format("Interface must be annotated with @%s!",
			RepositoryDefinition.class.getName());

	private final Class<?> repositoryInterface;

	/**
	 * Creates a new {@link AnnotationRepositoryMetadata} instance looking up repository types from a
	 * {@link RepositoryDefinition} annotation.
	 * 
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public AnnotationRepositoryMetadata(Class<?> repositoryInterface) {
		super(repositoryInterface);
		Assert.isTrue(repositoryInterface.isAnnotationPresent(RepositoryDefinition.class), NO_ANNOTATION_FOUND);
		this.repositoryInterface = repositoryInterface;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getIdClass()
	 */
	public Class<? extends Serializable> getIdType() {
		RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);
		return annotation == null ? null : annotation.idClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getDomainClass()
	 */
	public Class<?> getDomainType() {
		RepositoryDefinition annotation = repositoryInterface.getAnnotation(RepositoryDefinition.class);
		return annotation == null ? null : annotation.domainClass();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.support.RepositoryMetadata#getRepositoryInterface()
	 */
	public Class<?> getRepositoryInterface() {
		return repositoryInterface;
	}
}
