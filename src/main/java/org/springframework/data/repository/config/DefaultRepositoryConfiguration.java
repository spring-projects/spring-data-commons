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

import org.springframework.beans.factory.config.BeanDefinition;
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

	public static final String DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX = "Impl";
	private static final Key DEFAULT_QUERY_LOOKUP_STRATEGY = Key.CREATE_IF_NOT_FOUND;

	private final T configurationSource;
	private final BeanDefinition definition;

	/**
	 * Creates a new {@link DefaultRepositoryConfiguration} from the given {@link RepositoryConfigurationSource} and
	 * source {@link BeanDefinition}.
	 * 
	 * @param configurationSource must not be {@literal null}.
	 * @param definition must not be {@literal null}.
	 */
	public DefaultRepositoryConfiguration(T configurationSource, BeanDefinition definition) {

		Assert.notNull(configurationSource);
		Assert.notNull(definition);

		this.configurationSource = configurationSource;
		this.definition = definition;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getBeanId()
	 */
	public String getBeanId() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(getRepositoryFactoryBeanName()));
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
	public String getNamedQueriesLocation() {
		return configurationSource.getNamedQueryLocation();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getImplementationClassName()
	 */
	public String getImplementationClassName() {
		return ClassUtils.getShortName(getRepositoryInterface()) + getImplementationPostfix();
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfiguration#getRepositoryBaseClassName()
	 */
	@Override
	public String getRepositoryBaseClassName() {
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
