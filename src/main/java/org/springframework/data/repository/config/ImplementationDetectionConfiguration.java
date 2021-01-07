/*
 * Copyright 2018-2021 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Expresses configuration to be used to detect implementation classes for repositories and repository fragments.
 *
 * @author Oliver Gierke
 * @since 2.1
 */
public interface ImplementationDetectionConfiguration {

	/**
	 * Returns the postfix to be used to calculate the implementation type's name.
	 *
	 * @return must not be {@literal null}.
	 */
	String getImplementationPostfix();

	/**
	 * Return the base packages to be scanned for implementation types.
	 *
	 * @return must not be {@literal null}.
	 */
	Streamable<String> getBasePackages();

	/**
	 * Returns the exclude filters to be used for the implementation class scanning.
	 *
	 * @return must not be {@literal null}.
	 */
	Streamable<TypeFilter> getExcludeFilters();

	/**
	 * Returns the {@link MetadataReaderFactory} to be used for implementation class scanning.
	 *
	 * @return must not be {@literal null}.
	 */
	MetadataReaderFactory getMetadataReaderFactory();

	/**
	 * Generate the bean name for the given {@link BeanDefinition}.
	 *
	 * @param definition must not be {@literal null}.
	 * @return
	 */
	default String generateBeanName(BeanDefinition definition) {

		Assert.notNull(definition, "BeanDefinition must not be null!");

		String beanName = definition.getBeanClassName();

		if (beanName == null) {
			throw new IllegalStateException("Cannot generate bean name for BeanDefinition without bean class name!");
		}

		return Introspector.decapitalize(ClassUtils.getShortName(beanName));
	}

	/**
	 * Returns the final lookup configuration for the given fully-qualified fragment interface name.
	 *
	 * @param fragmentInterfaceName must not be {@literal null} or empty.
	 * @return
	 */
	default ImplementationLookupConfiguration forFragment(String fragmentInterfaceName) {

		Assert.hasText(fragmentInterfaceName, "Fragment interface name must not be null or empty!");

		return new DefaultImplementationLookupConfiguration(this, fragmentInterfaceName);
	}

	/**
	 * Returns the final lookup configuration for the given {@link RepositoryConfiguration}.
	 *
	 * @param config must not be {@literal null}.
	 * @return
	 */
	default ImplementationLookupConfiguration forRepositoryConfiguration(RepositoryConfiguration<?> config) {

		Assert.notNull(config, "RepositoryConfiguration must not be null!");

		return new DefaultImplementationLookupConfiguration(this, config.getRepositoryInterface()) {

			/*
			 * (non-Javadoc)
			 * @see org.springframework.data.repository.config.DefaultImplementationLookupConfiguration#getBasePackages()
			 */
			@Override
			public Streamable<String> getBasePackages() {
				return config.getImplementationBasePackages();
			}
		};
	}
}
