/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * Interface containing the configurable options for the Spring Data repository subsystem.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
 */
public interface RepositoryConfigurationSource {

	/**
	 * Returns the actual source object that the configuration originated from. Will be used by the tooling to give visual
	 * feedback on where the repository instances actually come from. @return.
	 */
	@Nullable
	Object getSource();

	/**
	 * Returns the base packages the repository interfaces shall be found under.
	 *
	 * @return must not be {@literal null}.
	 */
	Streamable<String> getBasePackages();

	/**
	 * Returns the {@link QueryLookupStrategy.Key} to define how query methods shall be resolved.
	 *
	 * @return
	 */
	Optional<Object> getQueryLookupStrategyKey();

	/**
	 * Returns the configured postfix to be used for looking up custom implementation classes.
	 *
	 * @return the postfix to use or {@link Optional#empty()} in case none is configured.
	 */
	Optional<String> getRepositoryImplementationPostfix();

	/**
	 * @return
	 */
	Optional<String> getNamedQueryLocation();

	/**
	 * Returns the name of the repository base class to be used or {@link Optional#empty()} if the store specific defaults
	 * shall be applied.
	 *
	 * @return
	 * @since 1.11
	 */
	Optional<String> getRepositoryBaseClassName();

	/**
	 * Returns the name of the repository factory bean class or {@link Optional#empty()} if not defined in the source.
	 *
	 * @return
	 */
	Optional<String> getRepositoryFactoryBeanClassName();

	/**
	 * Returns the source {@link BeanDefinition}s of the repository interfaces to create repository instances for.
	 *
	 * @param loader
	 * @return
	 */
	Streamable<BeanDefinition> getCandidates(ResourceLoader loader);

	/**
	 * Returns the value for the {@link String} attribute with the given name. The name is expected to be handed in
	 * camel-case.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @return the attribute with the given name or {@link Optional#empty()} if not configured or empty.
	 * @since 1.8
	 */
	Optional<String> getAttribute(String name);

	/**
	 * Returns whether the configuration uses explicit filtering to scan for repository types.
	 *
	 * @return whether the configuration uses explicit filtering to scan for repository types.
	 * @since 1.9
	 */
	boolean usesExplicitFilters();

	/**
	 * Return the {@link TypeFilter}s to define which types to exclude when scanning for repositories or repository
	 * implementations.
	 *
	 * @return must not be {@literal null}.
	 */
	Streamable<TypeFilter> getExcludeFilters();

	/**
	 * Returns a name for the beanDefinition.
	 *
	 * @param beanDefinition must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	String generateBeanName(BeanDefinition beanDefinition);
}
