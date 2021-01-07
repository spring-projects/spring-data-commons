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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
public class DefaultRepositoryConfiguration<T extends RepositoryConfigurationSource>
		implements RepositoryConfiguration<T> {

	public static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";
	public static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;

	private final T configurationSource;
	private final BeanDefinition definition;
	private final RepositoryConfigurationExtension extension;

	public DefaultRepositoryConfiguration(T configurationSource, BeanDefinition definition,
			RepositoryConfigurationExtension extension) {

		this.configurationSource = configurationSource;
		this.definition = definition;
		this.extension = extension;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getBeanId()
	 */
	public String getBeanId() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(getRepositoryBaseClassName().orElseThrow(
				() -> new IllegalStateException("Can't create bean identifier without a repository base class defined!"))));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getQueryLookupStrategyKey()
	 */
	public Object getQueryLookupStrategyKey() {
		return configurationSource.getQueryLookupStrategyKey().orElse(DEFAULT_QUERY_LOOKUP_STRATEGY);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getBasePackages()
	 */
	public Streamable<String> getBasePackages() {
		return configurationSource.getBasePackages();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationBasePackages()
	 */
	@Override
	public Streamable<String> getImplementationBasePackages() {
		return Streamable.of(ClassUtils.getPackageName(getRepositoryInterface()));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryInterface()
	 */
	public String getRepositoryInterface() {
		return ConfigurationUtils.getRequiredBeanClassName(definition);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getConfigSource()
	 */
	public RepositoryConfigurationSource getConfigSource() {
		return configurationSource;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getNamedQueryLocation()
	 */
	public Optional<String> getNamedQueriesLocation() {
		return configurationSource.getNamedQueryLocation();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationClassName()
	 */
	public String getImplementationClassName() {
		return ClassUtils.getShortName(getRepositoryInterface()).concat(
				configurationSource.getRepositoryImplementationPostfix().orElse(DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationBeanName()
	 */
	public String getImplementationBeanName() {
		return configurationSource.generateBeanName(definition)
				+ configurationSource.getRepositoryImplementationPostfix().orElse("Impl");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getSource()
	 */
	@Nullable
	@Override
	public Object getSource() {
		return configurationSource.getSource();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getConfigurationSource()
	 */
	@Override
	public T getConfigurationSource() {
		return configurationSource;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryBaseClassName()
	 */
	@Override
	public Optional<String> getRepositoryBaseClassName() {
		return configurationSource.getRepositoryBaseClassName();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryFactoryBeanClassName()
	 */
	@Override
	public String getRepositoryFactoryBeanClassName() {

		return configurationSource.getRepositoryFactoryBeanClassName()
				.orElseGet(extension::getRepositoryFactoryBeanClassName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#isLazyInit()
	 */
	@Override
	public boolean isLazyInit() {
		return definition.isLazyInit() || !configurationSource.getBootstrapMode().equals(BootstrapMode.DEFAULT);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#isPrimary()
	 */
	@Override
	public boolean isPrimary() {
		return definition.isPrimary();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getExcludeFilters()
	 */
	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return configurationSource.getExcludeFilters();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#toImplementationDetectionConfiguration(org.springframework.core.type.classreading.MetadataReaderFactory)
	 */
	@Override
	public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null!");

		return configurationSource.toImplementationDetectionConfiguration(factory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#toLookupConfiguration(org.springframework.core.type.classreading.MetadataReaderFactory)
	 */
	@Override
	public ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null!");

		return toImplementationDetectionConfiguration(factory).forRepositoryConfiguration(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getResourceDescription()
	 */
	@Override
	@org.springframework.lang.NonNull
	public String getResourceDescription() {
		return String.format("%s defined in %s", getRepositoryInterface(), configurationSource.getResourceDescription());
	}
}
