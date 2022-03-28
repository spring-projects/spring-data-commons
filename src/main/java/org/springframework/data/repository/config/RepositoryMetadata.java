/*
 * Copyright 2022 the original author or authors.
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

import java.util.List;
import java.util.Optional;

import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.0
 */
public class RepositoryMetadata<T extends RepositoryConfigurationSource> implements RepositoryConfiguration<T> {

	RepositoryConfiguration<T> repositoryConfiguration;
	List<RepositoryFragmentConfiguration> fragmentConfiguration;

	public RepositoryMetadata(RepositoryConfiguration<T> repositoryConfiguration,
			List<RepositoryFragmentConfiguration> fragmentConfiguration) {

		this.repositoryConfiguration = repositoryConfiguration;
		this.fragmentConfiguration = fragmentConfiguration;
	}

	@Override
	public Streamable<String> getBasePackages() {
		return repositoryConfiguration.getBasePackages();
	}

	@Override
	public Streamable<String> getImplementationBasePackages() {
		return repositoryConfiguration.getImplementationBasePackages();
	}

	@Override
	public String getRepositoryInterface() {
		return repositoryConfiguration.getRepositoryInterface();
	}

	@Override
	public Object getQueryLookupStrategyKey() {
		return repositoryConfiguration.getQueryLookupStrategyKey();
	}

	@Override
	public Optional<String> getNamedQueriesLocation() {
		return repositoryConfiguration.getNamedQueriesLocation();
	}

	@Override
	public Optional<String> getRepositoryBaseClassName() {
		return repositoryConfiguration.getRepositoryBaseClassName();
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return repositoryConfiguration.getRepositoryFactoryBeanClassName();
	}

	@Override
	@Nullable
	public Object getSource() {
		return repositoryConfiguration.getSource();
	}

	@Override
	public T getConfigurationSource() {
		return repositoryConfiguration.getConfigurationSource();
	}

	@Override
	public boolean isLazyInit() {
		return repositoryConfiguration.isLazyInit();
	}

	@Override
	public boolean isPrimary() {
		return repositoryConfiguration.isPrimary();
	}

	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return repositoryConfiguration.getExcludeFilters();
	}

	@Override
	public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {
		return repositoryConfiguration.toImplementationDetectionConfiguration(factory);
	}

	@Override
	public ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory) {
		return repositoryConfiguration.toLookupConfiguration(factory);
	}

	@Override
	@Nullable
	public String getResourceDescription() {
		return repositoryConfiguration.getResourceDescription();
	}

	public List<RepositoryFragmentConfiguration> getFragmentConfiguration() {
		return fragmentConfiguration;
	}
}
