/*
 * Copyright 2012-2015 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 * 
 * @author Oliver Gierke
 */
@RequiredArgsConstructor
public class DefaultRepositoryConfiguration<T extends RepositoryConfigurationSource>
		implements RepositoryConfiguration<T> {

	public static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";
	private static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;

	private final @NonNull T configurationSource;
	private final @NonNull BeanDefinition definition;

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
	public Iterable<String> getBasePackages() {
		return configurationSource.getBasePackages();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryInterface()
	 */
	public String getRepositoryInterface() {
		return definition.getBeanClassName();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getConfigSource()
	 */
	public RepositoryConfigurationSource getConfigSource() {
		return configurationSource;
	}

	/* (non-Javadoc)
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
		return StringUtils.uncapitalize(getImplementationClassName());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getSource()
	 */
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
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#isLazyInit()
	 */
	@Override
	public boolean isLazyInit() {
		return definition.isLazyInit();
	}
}
