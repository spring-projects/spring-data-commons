/*
 * Copyright 2012-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
public class DefaultRepositoryConfiguration<T extends RepositoryConfigurationSource>
		implements RepositoryConfiguration<T> {

	public static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";
	public static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;

	private final T configurationSource;
	private final BeanDefinition definition;
	private final RepositoryConfigurationExtension extension;
	private final Lazy<String> beanName;

	public DefaultRepositoryConfiguration(T configurationSource, BeanDefinition definition,
			RepositoryConfigurationExtension extension) {

		this.configurationSource = configurationSource;
		this.definition = definition;
		this.extension = extension;
		this.beanName = Lazy.of(() -> configurationSource.generateBeanName(definition));
	}

	public String getBeanId() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(getRepositoryBaseClassName().orElseThrow(
				() -> new IllegalStateException("Can't create bean identifier without a repository base class defined"))));
	}

	@Override
	public Object getQueryLookupStrategyKey() {
		return configurationSource.getQueryLookupStrategyKey().orElse(DEFAULT_QUERY_LOOKUP_STRATEGY);
	}

	@Override
	public Streamable<String> getBasePackages() {
		return configurationSource.getBasePackages();
	}

	@Override
	public Streamable<String> getImplementationBasePackages() {
		return Streamable.of(ClassUtils.getPackageName(getRepositoryInterface()));
	}

	@Override
	public String getRepositoryInterface() {
		return ConfigurationUtils.getRequiredBeanClassName(definition);
	}

	public RepositoryConfigurationSource getConfigSource() {
		return configurationSource;
	}

	@Override
	public Optional<String> getNamedQueriesLocation() {
		return configurationSource.getNamedQueryLocation();
	}

	public String getImplementationClassName() {
		return ClassUtils.getShortName(getRepositoryInterface()).concat(
				configurationSource.getRepositoryImplementationPostfix().orElse(DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX));
	}

	@Override
	public String getImplementationBeanName() {
		return beanName.get() + configurationSource.getRepositoryImplementationPostfix().orElse("Impl");
	}

	@Override
	public @Nullable Object getSource() {
		return configurationSource.getSource();
	}

	@Override
	public T getConfigurationSource() {
		return configurationSource;
	}

	@Override
	public Optional<String> getRepositoryBaseClassName() {
		return configurationSource.getRepositoryBaseClassName()
				.or(() -> Optional.ofNullable(extension.getRepositoryBaseClassName()));
	}

	@Override
	public Optional<String> getRepositoryFragmentsContributorClassName() {
		return configurationSource.getRepositoryFragmentsContributorClassName();
	}

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return configurationSource.getRepositoryFactoryBeanClassName()
				.orElseGet(extension::getRepositoryFactoryBeanClassName);
	}

	@Override
	public String getRepositoryBeanName() {
		return beanName.get();
	}

	@Override
	public boolean isLazyInit() {
		return definition.isLazyInit() || !configurationSource.getBootstrapMode().equals(BootstrapMode.DEFAULT);
	}

	@Override
	public boolean isPrimary() {
		return definition.isPrimary();
	}

	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return configurationSource.getExcludeFilters();
	}

	@Override
	public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null");

		return configurationSource.toImplementationDetectionConfiguration(factory);
	}

	@Override
	public ImplementationLookupConfiguration toLookupConfiguration(MetadataReaderFactory factory) {

		Assert.notNull(factory, "MetadataReaderFactory must not be null");

		return toImplementationDetectionConfiguration(factory).forRepositoryConfiguration(this);
	}

	@Override
	@org.jspecify.annotations.NonNull
	public String getResourceDescription() {
		return String.format("%s defined in %s", getRepositoryInterface(), configurationSource.getResourceDescription());
	}
}
