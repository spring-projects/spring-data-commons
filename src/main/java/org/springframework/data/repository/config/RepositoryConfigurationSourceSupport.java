/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

/**
 * Base class to implement {@link RepositoryConfigurationSource}s.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Peter Rietzler
 */
public abstract class RepositoryConfigurationSourceSupport implements RepositoryConfigurationSource {

	protected static final String DEFAULT_REPOSITORY_IMPL_POSTFIX = "Impl";

	private final Environment environment;
	private final BeanDefinitionRegistry registry;

	/**
	 * Creates a new {@link RepositoryConfigurationSourceSupport} with the given environment.
	 * 
	 * @param environment must not be {@literal null}.
	 */
	public RepositoryConfigurationSourceSupport(Environment environment, BeanDefinitionRegistry registry) {

		Assert.notNull(environment, "Environment must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		this.environment = environment;
		this.registry = registry;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getCandidates(org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider)
	 */
	public Collection<BeanDefinition> getCandidates(ResourceLoader loader) {

		RepositoryComponentProvider scanner = new RepositoryComponentProvider(getIncludeFilters(), registry);
		scanner.setConsiderNestedRepositoryInterfaces(shouldConsiderNestedRepositories());
		scanner.setEnvironment(environment);
		scanner.setResourceLoader(loader);

		for (TypeFilter filter : getExcludeFilters()) {
			scanner.addExcludeFilter(filter);
		}

		Set<BeanDefinition> result = new HashSet<BeanDefinition>();

		for (String basePackage : getBasePackages()) {
			Set<BeanDefinition> candidate = scanner.findCandidateComponents(basePackage);
			result.addAll(candidate);
		}

		return result;
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to exclude when scanning for repositories. Default
	 * implementation returns an empty collection.
	 * 
	 * @return must not be {@literal null}.
	 */
	public Iterable<TypeFilter> getExcludeFilters() {
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

	/**
	 * Returns whether we should consider nested repositories, i.e. repository interface definitions nested in other
	 * classes.
	 * 
	 * @return {@literal true} if the container should look for nested repository interface definitions.
	 */
	public boolean shouldConsiderNestedRepositories() {
		return false;
	}
}
