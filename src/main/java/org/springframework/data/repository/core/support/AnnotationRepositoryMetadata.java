/*
 * Copyright 2011-2022 the original author or authors.
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

import java.util.function.Function;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link RepositoryMetadata} implementation inspecting the given repository interface for a
 * {@link RepositoryDefinition} annotation.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Xeno Amess
 * @author Alessandro Nistico
 * @author Johannes Englmeier
 */
public class AnnotationRepositoryMetadata extends AbstractRepositoryMetadata {

	private static final String NO_ANNOTATION_FOUND = String.format("Interface %%s must be annotated with @%s!",
			RepositoryDefinition.class.getName());

	private final TypeInformation<?> idType;
	private final TypeInformation<?> domainType;

	/**
	 * Creates a new {@link AnnotationRepositoryMetadata} instance looking up repository types from a
	 * {@link RepositoryDefinition} annotation.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public AnnotationRepositoryMetadata(Class<?> repositoryInterface) {

		super(repositoryInterface);

		Assert.isTrue(AnnotationUtils.findAnnotation(repositoryInterface, RepositoryDefinition.class) != null,
				() -> String.format(NO_ANNOTATION_FOUND, repositoryInterface.getName()));

		this.idType = resolveType(repositoryInterface, RepositoryDefinition::idClass);
		this.domainType = resolveType(repositoryInterface, RepositoryDefinition::domainClass);
	}

	@Override
	public TypeInformation<?> getIdTypeInformation() {
		return this.idType;
	}

	@Override
	public TypeInformation<?> getDomainTypeInformation() {
		return this.domainType;
	}

	private static TypeInformation<?> resolveType(Class<?> repositoryInterface,
			Function<RepositoryDefinition, Class<?>> extractor) {

		RepositoryDefinition annotation = AnnotationUtils.findAnnotation(repositoryInterface, RepositoryDefinition.class);

		if ((annotation == null) || (extractor.apply(annotation) == null)) {
			throw new IllegalArgumentException(String.format("Could not resolve domain type of %s", repositoryInterface));
		}

		return ClassTypeInformation.from(extractor.apply(annotation));
	}
}
