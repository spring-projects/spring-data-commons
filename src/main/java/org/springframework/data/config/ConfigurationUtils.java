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
package org.springframework.data.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Helper class to centralize common functionality that needs to be used in various places of the configuration
 * implementation.
 *
 * @author Oliver Gierke
 * @since 2.0
 * @soundtrack Richard Spaven - The Self (feat. Jordan Rakei)
 */
public interface ConfigurationUtils {

	/**
	 * Returns the {@link ResourceLoader} from the given {@link XmlReaderContext}.
	 *
	 * @param context must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException if no {@link ResourceLoader} can be obtained from the {@link XmlReaderContext}.
	 */
	public static ResourceLoader getRequiredResourceLoader(XmlReaderContext context) {

		Assert.notNull(context, "XmlReaderContext must not be null!");

		ResourceLoader resourceLoader = context.getResourceLoader();

		if (resourceLoader == null) {
			throw new IllegalArgumentException("Could not obtain ResourceLoader from XmlReaderContext!");
		}

		return resourceLoader;
	}

	/**
	 * Returns the {@link ClassLoader} used by the given {@link XmlReaderContext}.
	 *
	 * @param context must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException if no {@link ClassLoader} can be obtained from the given {@link XmlReaderContext}.
	 */
	public static ClassLoader getRequiredClassLoader(XmlReaderContext context) {
		return getRequiredClassLoader(getRequiredResourceLoader(context));
	}

	/**
	 * Returns the {@link ClassLoader} used by the given {@link ResourceLoader}.
	 *
	 * @param resourceLoader must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException if the given {@link ResourceLoader} does not expose a {@link ClassLoader}.
	 */
	public static ClassLoader getRequiredClassLoader(ResourceLoader resourceLoader) {

		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");

		ClassLoader classLoader = resourceLoader.getClassLoader();

		if (classLoader == null) {
			throw new IllegalArgumentException("Could not obtain ClassLoader from ResourceLoader!");
		}

		return classLoader;
	}

	/**
	 * Returns the bean class name of the given {@link BeanDefinition}.
	 *
	 * @param beanDefinition must not be {@literal null}.
	 * @return
	 * @throws IllegalArgumentException if the given {@link BeanDefinition} does not contain a bean class name.
	 */
	public static String getRequiredBeanClassName(BeanDefinition beanDefinition) {

		Assert.notNull(beanDefinition, "BeanDefinition must not be null!");

		String result = beanDefinition.getBeanClassName();

		if (result == null) {
			throw new IllegalArgumentException(
					String.format("Could not obtain required bean class name from BeanDefinition!", beanDefinition));
		}

		return result;
	}
}
