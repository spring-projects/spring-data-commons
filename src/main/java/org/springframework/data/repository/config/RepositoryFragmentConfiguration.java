/*
 * Copyright 2017-2021 the original author or authors.
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

import java.beans.Introspector;
import java.util.Optional;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Fragment configuration consisting of an interface name and the implementation class name.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 */
public final class RepositoryFragmentConfiguration {

	private final String interfaceName;
	private final String className;
	private final Optional<AbstractBeanDefinition> beanDefinition;

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

	public RepositoryFragmentConfiguration(String interfaceName, String className,
			Optional<AbstractBeanDefinition> beanDefinition) {
		this.interfaceName = interfaceName;
		this.className = className;
		this.beanDefinition = beanDefinition;
	}

	/**
	 * @return name of the implementation bean.
	 */
	public String getImplementationBeanName() {
		return Introspector.decapitalize(ClassUtils.getShortName(getClassName()));
	}

	/**
	 * @return name of the implementation fragment bean.
	 */
	public String getFragmentBeanName() {
		return getImplementationBeanName() + "Fragment";
	}

	public String getInterfaceName() {
		return this.interfaceName;
	}

	public String getClassName() {
		return this.className;
	}

	public Optional<AbstractBeanDefinition> getBeanDefinition() {
		return this.beanDefinition;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof RepositoryFragmentConfiguration)) {
			return false;
		}

		RepositoryFragmentConfiguration that = (RepositoryFragmentConfiguration) o;
		if (!ObjectUtils.nullSafeEquals(interfaceName, that.interfaceName)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(className, that.className)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(beanDefinition, that.beanDefinition);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(interfaceName);
		result = 31 * result + ObjectUtils.nullSafeHashCode(className);
		result = 31 * result + ObjectUtils.nullSafeHashCode(beanDefinition);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RepositoryFragmentConfiguration(interfaceName=" + this.getInterfaceName() + ", className="
				+ this.getClassName() + ", beanDefinition=" + this.getBeanDefinition() + ")";
	}
}
