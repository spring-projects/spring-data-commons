/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Base class to implement {@link RepositoryConfigurationSource}s.
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryConfigurationSourceSupport implements RepositoryConfigurationSource {

	protected static final String DEFAULT_REPOSITORY_IMPL_POSTFIX = "Impl";

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getCandidates(org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider)
	 */
	public Collection<String> getCandidates(ResourceLoader loader) {

		ClassPathScanningCandidateComponentProvider scanner = new RepositoryComponentProvider(getIncludeFilters());
		scanner.setResourceLoader(loader);

		for (TypeFilter filter : getExcludeFilters()) {
			scanner.addExcludeFilter(filter);
		}

		Set<String> result = new HashSet<String>();

		for (String basePackage : getBasePackages()) {
			Collection<BeanDefinition> components = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition definition : components) {
				result.add(definition.getBeanClassName());
			}
		}

		return result;
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to exclude when scanning for repositories. Default
	 * implementation returns an empty collection.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected Iterable<TypeFilter> getExcludeFilters() {
		return Collections.emptySet();
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to include when scanning for repositories. Default
	 * implementation returns an empty collection.
	 * 
	 * @return must not be {@literal null}.
	 */
	protected Iterable<TypeFilter> getIncludeFilters() {
		return Collections.emptySet();
	}
}
