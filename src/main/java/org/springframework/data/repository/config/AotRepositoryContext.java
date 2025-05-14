/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.aot.AotContext;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * {@link AotContext} specific to Spring Data {@link org.springframework.data.repository.Repository} infrastructure.
 *
 * @author Christoph Strobl
 * @author John Blum
 * @author Mark Paluch
 * @since 3.0
 * @see AotContext
 */
public interface AotRepositoryContext extends AotContext {

	/**
	 * @return the {@link String bean name} of the repository / factory bean.
	 */
	String getBeanName();

	/**
	 * @return the Spring Data module name, see {@link RepositoryConfigurationExtension#getModuleName()}.
	 * @since 4.0
	 */
	String getModuleName();

	/**
	 * @return the repository configuration source.
	 */
	RepositoryConfigurationSource getConfigurationSource();

	/**
	 * @return a {@link Set} of {@link String base packages} to search for repositories.
	 */
	default Set<String> getBasePackages() {
		return getConfigurationSource().getBasePackages().toSet();
	}

	/**
	 * @return the {@link Annotation} types used to identify domain types.
	 */
	Collection<Class<? extends Annotation>> getIdentifyingAnnotations();

	/**
	 * @return {@link RepositoryInformation metadata} about the repository itself.
	 * @see org.springframework.data.repository.core.RepositoryInformation
	 */
	RepositoryInformation getRepositoryInformation();

	/**
	 * @return all {@link MergedAnnotation annotations} reachable from the repository.
	 * @see org.springframework.core.annotation.MergedAnnotation
	 */
	Set<MergedAnnotation<Annotation>> getResolvedAnnotations();

	/**
	 * @return all {@link Class types} reachable from the repository.
	 */
	Set<Class<?>> getResolvedTypes();

}
