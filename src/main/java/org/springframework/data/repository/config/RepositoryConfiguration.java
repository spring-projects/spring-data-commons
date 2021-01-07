/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Optional;

import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * Configuration information for a single repository instance.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public interface RepositoryConfiguration<T extends RepositoryConfigurationSource> {

	/**
	 * Returns the base packages that the repository was scanned under.
	 *
	 * @return
	 */
	Streamable<String> getBasePackages();

	/**
	 * Returns the base packages to scan for repository implementations.
	 *
	 * @return
	 * @since 2.0
	 */
	Streamable<String> getImplementationBasePackages();

	/**
	 * Returns the interface name of the repository.
	 *
	 * @return
	 */
	String getRepositoryInterface();

	/**
	 * Returns the key to resolve a {@link QueryLookupStrategy} from eventually.
	 *
	 * @see QueryLookupStrategy.Key
	 * @return
	 */
	Object getQueryLookupStrategyKey();

	/**
	 * Returns the location of the file containing Spring Data named queries.
	 *
	 * @return
	 */
	Optional<String> getNamedQueriesLocation();

	/**
	 * Returns the name of the repository base class to be used or {@literal null} if the store specific defaults shall be
	 * applied.
	 *
	 * @return
	 * @since 1.11
	 */
	Optional<String> getRepositoryBaseClassName();

	/**
	 * Returns the name of the repository factory bean class to be used.
	 *
	 * @return
	 */
	String getRepositoryFactoryBeanClassName();

	/**
	 * Returns the source of the {@link RepositoryConfiguration}.
	 *
	 * @return
	 */
	@Nullable
	Object getSource();

	/**
	 * Returns the {@link RepositoryConfigurationSource} that backs the {@link RepositoryConfiguration}.
	 *
	 * @return
	 */
	T getConfigurationSource();

	/**
	 * Returns whether to initialize the repository proxy lazily.
	 *
	 * @return
	 */
	boolean isLazyInit();

	/**
	 * Returns whether the repository is the primary one for its type.
	 *
	 * @return {@literal true} whether the repository is the primary one for its type.
	 * @since 2.3
	 */
	boolean isPrimary();

	/**
	 * Returns the {@link TypeFilter}s to be used to exclude packages from repository scanning.
	 *
	 * @return
	 */
	Streamable<TypeFilter> getExcludeFilters();

	/**
	 * Returns the {@link ImplementationDetectionConfiguration} to be used for this repository.
	 *
	 * @param factory must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 2.1
	 */
	ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory);

	/**
	 * Returns the {@link ImplementationLookupConfiguration} for the given {@link MetadataReaderFactory}.
	 *
	 * @param factory must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 2.1
	 */
	ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory);

	/**
	 * Returns a human readable description of the repository interface declaration for error reporting purposes.
	 *
	 * @return can be {@literal null}.
	 * @since 2.3
	 */
	@Nullable
	String getResourceDescription();
}
