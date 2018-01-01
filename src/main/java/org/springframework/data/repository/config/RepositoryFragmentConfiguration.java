/*
 * Copyright 2017-2018 the original author or authors.
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

import lombok.Value;

import java.util.Optional;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Fragment configuration consisting of an interface name and the implementation class name.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 */
@Value
public class RepositoryFragmentConfiguration {

	String interfaceName, className;
	Optional<AbstractBeanDefinition> beanDefinition;

	/**
	 * Creates a {@link RepositoryFragmentConfiguration} given {@code interfaceName} and {@code className} of the
	 * implementation.
	 *
	 * @param interfaceName must not be {@literal null} or empty.
	 * @param className must not be {@literal null} or empty.
	 */
	public RepositoryFragmentConfiguration(String interfaceName, String className) {

		Assert.hasText(interfaceName, "Interface name must not be null or empty!");
		Assert.hasText(className, "Class name must not be null or empty!");

		this.interfaceName = interfaceName;
		this.className = className;
		this.beanDefinition = Optional.empty();
	}

	/**
	 * Creates a {@link RepositoryFragmentConfiguration} given {@code interfaceName} and {@link AbstractBeanDefinition} of
	 * the implementation.
	 *
	 * @param interfaceName must not be {@literal null} or empty.
	 * @param beanDefinition must not be {@literal null}.
	 */
	public RepositoryFragmentConfiguration(String interfaceName, AbstractBeanDefinition beanDefinition) {

		Assert.hasText(interfaceName, "Interface name must not be null or empty!");
		Assert.notNull(beanDefinition, "Bean definition must not be null!");

		this.interfaceName = interfaceName;
		this.className = ConfigurationUtils.getRequiredBeanClassName(beanDefinition);
		this.beanDefinition = Optional.of(beanDefinition);
	}

	/**
	 * @return name of the implementation bean.
	 */
	public String getImplementationBeanName() {
		return StringUtils.uncapitalize(ClassUtils.getShortName(getClassName()));
	}

	/**
	 * @return name of the implementation fragment bean.
	 */
	public String getFragmentBeanName() {
		return getImplementationBeanName() + "Fragment";
	}
}
