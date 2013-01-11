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

import java.util.Collection;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.io.ResourceLoader;

/**
 * SPI to implement store specific extension to the repository bean definition registration process.
 * 
 * @see RepositoryConfigurationExtensionSupport
 * @author Oliver Gierke
 */
public interface RepositoryConfigurationExtension {

	/**
	 * Returns all {@link RepositoryConfiguration}s obtained through the given {@link RepositoryConfigurationSource}.
	 * 
	 * @param configSource must not be {@literal null}.
	 * @param loader must not be {@literal null}.
	 * @return
	 */
	<T extends RepositoryConfigurationSource> Collection<RepositoryConfiguration<T>> getRepositoryConfigurations(
			T configSource, ResourceLoader loader);

	/**
	 * Returns the default location of the Spring Data named queries.
	 * 
	 * @return must not be {@literal null} or empty.
	 */
	String getDefaultNamedQueryLocation();

	/**
	 * Returns the name of the repository factory class to be used.
	 * 
	 * @return
	 */
	String getRepositoryFactoryClassName();

	/**
	 * Callback to register additional bean definitions for a {@literal repositories} root node. This usually includes
	 * beans you have to set up once independently of the number of repositories to be created. Will be called before any
	 * repositories bean definitions have been registered.
	 * 
	 * @param registry
	 * @param source
	 */
	void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource configurationSource);

	/**
	 * Callback to post process the {@link BeanDefinition} built from annotations and tweak the configuration if
	 * necessary.
	 * 
	 * @param builder will never be {@literal null}.
	 * @param config will never be {@literal null}.
	 */
	void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config);

	/**
	 * Callback to post process the {@link BeanDefinition} built from XML and tweak the configuration if necessary.
	 * 
	 * @param builder will never be {@literal null}.
	 * @param config will never be {@literal null}.
	 */
	void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config);
}
