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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.repository.query.ExtensionAwareEvaluationContextProvider;
import org.springframework.util.Assert;

/**
 * Builder to create {@link BeanDefinitionBuilder} instance to eventually create Spring Data repository instances.
 * 
 * @author Oliver Gierke
 */
class RepositoryBeanDefinitionBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanDefinitionBuilder.class);

	private final BeanDefinitionRegistry registry;
	private final RepositoryConfigurationExtension extension;
	private final ResourceLoader resourceLoader;

	private final MetadataReaderFactory metadataReaderFactory;
	private final CustomRepositoryImplementationDetector implementationDetector;

	/**
	 * Creates a new {@link RepositoryBeanDefinitionBuilder} from the given {@link BeanDefinitionRegistry},
	 * {@link RepositoryConfigurationExtension} and {@link ResourceLoader}.
	 * 
	 * @param registry must not be {@literal null}.
	 * @param extension must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 */
	public RepositoryBeanDefinitionBuilder(BeanDefinitionRegistry registry, RepositoryConfigurationExtension extension,
			ResourceLoader resourceLoader, Environment environment) {

		Assert.notNull(extension, "RepositoryConfigurationExtension must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		Assert.notNull(environment, "Environment must not be null!");

		this.registry = registry;
		this.extension = extension;
		this.resourceLoader = resourceLoader;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		this.implementationDetector = new CustomRepositoryImplementationDetector(metadataReaderFactory, environment,
				resourceLoader);
	}

	/**
	 * Builds a new {@link BeanDefinitionBuilder} from the given {@link BeanDefinitionRegistry} and {@link ResourceLoader}
	 * .
	 * 
	 * @param configuration must not be {@literal null}.
	 * @return
	 */
	public BeanDefinitionBuilder build(RepositoryConfiguration<?> configuration) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(extension.getRepositoryFactoryClassName());

		builder.getRawBeanDefinition().setSource(configuration.getSource());
		builder.addPropertyValue("repositoryInterface", configuration.getRepositoryInterface());
		builder.addPropertyValue("queryLookupStrategyKey", configuration.getQueryLookupStrategyKey());
		builder.addPropertyValue("lazyInit", configuration.isLazyInit());

		configuration.getRepositoryBaseClassName()//
				.ifPresent(it -> builder.addPropertyValue("repositoryBaseClass", it));

		NamedQueriesBeanDefinitionBuilder definitionBuilder = new NamedQueriesBeanDefinitionBuilder(
				extension.getDefaultNamedQueryLocation());
		configuration.getNamedQueriesLocation().ifPresent(it -> definitionBuilder.setLocations(it));

		builder.addPropertyValue("namedQueries", definitionBuilder.build(configuration.getSource()));

		registerCustomImplementation(configuration).ifPresent(it -> {
			builder.addPropertyReference("customImplementation", it);
			builder.addDependsOn(it);
		});

		RootBeanDefinition evaluationContextProviderDefinition = new RootBeanDefinition(
				ExtensionAwareEvaluationContextProvider.class);
		evaluationContextProviderDefinition.setSource(configuration.getSource());

		builder.addPropertyValue("evaluationContextProvider", evaluationContextProviderDefinition);

		return builder;
	}

	private Optional<String> registerCustomImplementation(RepositoryConfiguration<?> configuration) {

		String beanName = configuration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return Optional.of(beanName);
		}

		Optional<AbstractBeanDefinition> beanDefinition = implementationDetector
				.detectCustomImplementation(configuration.getImplementationClassName(), configuration.getBasePackages());

		return beanDefinition.map(it -> {

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Registering custom repository implementation: " + configuration.getImplementationBeanName() + " "
						+ it.getBeanClassName());
			}

			it.setSource(configuration.getSource());

			registry.registerBeanDefinition(beanName, it);

			return beanName;
		});
	}
}
