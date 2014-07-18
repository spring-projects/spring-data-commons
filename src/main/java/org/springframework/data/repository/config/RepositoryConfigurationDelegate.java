/*
 * Copyright 2014 the original author or authors.
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

import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Delegate for configuration integration to reuse the general way of detecting repositories. Customization is done by
 * providing a configuration format specific {@link RepositoryConfigurationSource} (currently either XML or annotations
 * are supported). The actual registration can then be triggered for different {@link RepositoryConfigurationExtension}
 * s.
 * 
 * @author Oliver Gierke
 */
public class RepositoryConfigurationDelegate {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryConfigurationDelegate.class);

	private final RepositoryConfigurationSource configurationSource;
	private final ResourceLoader resourceLoader;
	private final Environment environment;
	private final BeanNameGenerator generator;
	private final boolean isXml;

	/**
	 * Creates a new {@link RepositoryConfigurationDelegate} for the given {@link RepositoryConfigurationSource} and
	 * {@link ResourceLoader} and {@link Environment}.
	 * 
	 * @param configurationSource must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	public RepositoryConfigurationDelegate(RepositoryConfigurationSource configurationSource,
			ResourceLoader resourceLoader, Environment environment) {

		this.isXml = configurationSource instanceof XmlRepositoryConfigurationSource;
		boolean isAnnotation = configurationSource instanceof AnnotationRepositoryConfigurationSource;

		Assert.isTrue(isXml || isAnnotation,
				"Configuration source must either be an Xml- or an AnnotationBasedConfigurationSource!");
		Assert.notNull(resourceLoader);

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(resourceLoader.getClassLoader());

		this.generator = generator;
		this.configurationSource = configurationSource;
		this.resourceLoader = resourceLoader;
		this.environment = defaultEnvironment(environment, resourceLoader);
	}

	/**
	 * Defaults the environment in case the given one is null. Used as fallback, in case the legacy constructor was
	 * invoked.
	 * 
	 * @param environment can be {@literal null}.
	 * @param resourceLoader can be {@literal null}.
	 * @return
	 */
	private static Environment defaultEnvironment(Environment environment, ResourceLoader resourceLoader) {

		if (environment != null) {
			return environment;
		}

		return resourceLoader instanceof EnvironmentCapable ? ((EnvironmentCapable) resourceLoader).getEnvironment()
				: new StandardEnvironment();
	}

	/**
	 * Registers the found repositories in the given {@link BeanDefinitionRegistry}.
	 * 
	 * @param registry
	 * @param extension
	 * @return {@link BeanComponentDefinition}s for all repository bean definitions found.
	 */
	public List<BeanComponentDefinition> registerRepositoriesIn(BeanDefinitionRegistry registry,
			RepositoryConfigurationExtension extension) {

		exposeRegistration(extension, registry);

		extension.registerBeansForRoot(registry, configurationSource);

		RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(registry, extension, resourceLoader,
				environment);
		List<BeanComponentDefinition> definitions = new ArrayList<BeanComponentDefinition>();

		for (RepositoryConfiguration<? extends RepositoryConfigurationSource> configuration : extension
				.getRepositoryConfigurations(configurationSource, resourceLoader)) {

			BeanDefinitionBuilder definitionBuilder = builder.build(configuration);

			extension.postProcess(definitionBuilder, configurationSource);

			if (isXml) {
				extension.postProcess(definitionBuilder, (XmlRepositoryConfigurationSource) configurationSource);
			} else {
				extension.postProcess(definitionBuilder, (AnnotationRepositoryConfigurationSource) configurationSource);
			}

			AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
			String beanName = generator.generateBeanName(beanDefinition, registry);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Registering repository: " + beanName + " - Interface: " + configuration.getRepositoryInterface()
						+ " - Factory: " + extension.getRepositoryFactoryClassName());
			}

			registry.registerBeanDefinition(beanName, beanDefinition);
			definitions.add(new BeanComponentDefinition(beanDefinition, beanName));
		}

		return definitions;
	}

	/**
	 * Registeres the given {@link RepositoryConfigurationExtension} to indicate the repository configuration for a
	 * particular store (expressed through the extension's concrete type) has appened. Useful for downstream components
	 * that need to detect exactly that case. The bean definition is marked as lazy-init so that it doesn't get
	 * instantiated if no one really cares.
	 * 
	 * @param extension
	 * @param registry
	 */
	private void exposeRegistration(RepositoryConfigurationExtension extension, BeanDefinitionRegistry registry) {

		Class<? extends RepositoryConfigurationExtension> extensionType = extension.getClass();
		String beanName = extensionType.getName().concat(GENERATED_BEAN_NAME_SEPARATOR).concat("0");

		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		// Register extension as bean to indicate repository parsing and registration has happened
		RootBeanDefinition definition = new RootBeanDefinition(extensionType);
		definition.setSource(configurationSource.getSource());
		definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
		definition.setLazyInit(true);

		registry.registerBeanDefinition(beanName, definition);
	}
}
