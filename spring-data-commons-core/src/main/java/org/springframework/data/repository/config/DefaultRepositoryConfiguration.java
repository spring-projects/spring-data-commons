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

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 * 
 * @author Oliver Gierke
 */
public class DefaultRepositoryConfiguration<T extends RepositoryConfigurationSource> implements
		RepositoryConfiguration<T> {

	private static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;
	private static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";

	private final T configurationSource;
	private final String interfaceName;

	/**
	 * Creates a new {@link DefaultRepositoryConfiguration} from the given {@link RepositoryConfigurationSource} and
	 * interface name.
	 * 
	 * @param configurationSource must not be {@literal null}.
	 * @param interfaceName must not be {@literal null} or empty.
	 */
	public DefaultRepositoryConfiguration(T configurationSource, String interfaceName) {

		Assert.notNull(configurationSource);
		Assert.hasText(interfaceName);

		this.configurationSource = configurationSource;
		this.interfaceName = interfaceName;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getBeanId()
	 */
	public String getBeanId() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(interfaceName));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getQueryLookupStrategyKey()
	 */
	public Object getQueryLookupStrategyKey() {

		Object configuredStrategy = configurationSource.getQueryLookupStrategyKey();
		return configuredStrategy != null ? configuredStrategy : DEFAULT_QUERY_LOOKUP_STRATEGY;
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
		return interfaceName;
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
	public String getNamedQueriesLocation() {
		return configurationSource.getNamedQueryLocation();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationClassName()
	 */
	public String getImplementationClassName() {
		return ClassUtils.getShortName(interfaceName) + getImplementationPostfix();
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
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationPostfix()
	 */
	public String getImplementationPostfix() {

		String configuredPostfix = configurationSource.getRepositoryImplementationPostfix();
		return StringUtils.hasText(configuredPostfix) ? configuredPostfix : DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getSource()
	 */
	public Object getSource() {
		return configurationSource.getSource();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getConfigurationSource()
	 */
	public T getConfigurationSource() {
		return configurationSource;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return configurationSource.getRepositoryFactoryBeanName();
	}
}
