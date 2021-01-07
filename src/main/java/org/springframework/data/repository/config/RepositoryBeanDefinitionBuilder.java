/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsFactoryBean;
import org.springframework.data.util.Optionals;
import org.springframework.util.Assert;

/**
 * Builder to create {@link BeanDefinitionBuilder} instance to eventually create Spring Data repository instances.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Mark Paluch
 */
class RepositoryBeanDefinitionBuilder {

	private static final Log logger = LogFactory.getLog(RepositoryBeanDefinitionBuilder.class);

	private final BeanDefinitionRegistry registry;
	private final RepositoryConfigurationExtension extension;
	private final ResourceLoader resourceLoader;

	private final MetadataReaderFactory metadataReaderFactory;
	private final FragmentMetadata fragmentMetadata;
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
			RepositoryConfigurationSource configurationSource, ResourceLoader resourceLoader, Environment environment) {

		Assert.notNull(extension, "RepositoryConfigurationExtension must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		Assert.notNull(environment, "Environment must not be null!");

		this.registry = registry;
		this.extension = extension;
		this.resourceLoader = resourceLoader;

		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);

		this.fragmentMetadata = new FragmentMetadata(metadataReaderFactory);
		this.implementationDetector = new CustomRepositoryImplementationDetector(environment, resourceLoader,
				configurationSource.toImplementationDetectionConfiguration(metadataReaderFactory));
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

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(configuration.getRepositoryFactoryBeanClassName());

		builder.getRawBeanDefinition().setSource(configuration.getSource());
		builder.addConstructorArgValue(configuration.getRepositoryInterface());
		builder.addPropertyValue("queryLookupStrategyKey", configuration.getQueryLookupStrategyKey());
		builder.addPropertyValue("lazyInit", configuration.isLazyInit());
		builder.setLazyInit(configuration.isLazyInit());
		builder.setPrimary(configuration.isPrimary());

		configuration.getRepositoryBaseClassName()//
				.ifPresent(it -> builder.addPropertyValue("repositoryBaseClass", it));

		NamedQueriesBeanDefinitionBuilder definitionBuilder = new NamedQueriesBeanDefinitionBuilder(
				extension.getDefaultNamedQueryLocation());
		configuration.getNamedQueriesLocation().ifPresent(definitionBuilder::setLocations);

		builder.addPropertyValue("namedQueries", definitionBuilder.build(configuration.getSource()));

		registerCustomImplementation(configuration).ifPresent(it -> {
			builder.addPropertyReference("customImplementation", it);
			builder.addDependsOn(it);
		});

		BeanDefinitionBuilder fragmentsBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(RepositoryFragmentsFactoryBean.class);

		List<String> fragmentBeanNames = registerRepositoryFragmentsImplementation(configuration) //
				.map(RepositoryFragmentConfiguration::getFragmentBeanName) //
				.collect(Collectors.toList());

		fragmentsBuilder.addConstructorArgValue(fragmentBeanNames);

		builder.addPropertyValue("repositoryFragments",
				ParsingUtils.getSourceBeanDefinition(fragmentsBuilder, configuration.getSource()));

		return builder;
	}

	private Optional<String> registerCustomImplementation(RepositoryConfiguration<?> configuration) {

		ImplementationLookupConfiguration lookup = configuration.toLookupConfiguration(metadataReaderFactory);

		String beanName = lookup.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return Optional.of(beanName);
		}

		Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(lookup);

		return beanDefinition.map(it -> {

			if (logger.isDebugEnabled()) {
				logger.debug("Registering custom repository implementation: " + lookup.getImplementationBeanName() + " "
						+ it.getBeanClassName());
			}

			it.setSource(configuration.getSource());
			registry.registerBeanDefinition(beanName, it);

			return beanName;
		});
	}

	private Stream<RepositoryFragmentConfiguration> registerRepositoryFragmentsImplementation(
			RepositoryConfiguration<?> configuration) {

		ImplementationDetectionConfiguration config = configuration
				.toImplementationDetectionConfiguration(metadataReaderFactory);

		return fragmentMetadata.getFragmentInterfaces(configuration.getRepositoryInterface()) //
				.map(it -> detectRepositoryFragmentConfiguration(it, config)) //
				.flatMap(Optionals::toStream) //
				.peek(it -> potentiallyRegisterFragmentImplementation(configuration, it)) //
				.peek(it -> potentiallyRegisterRepositoryFragment(configuration, it));
	}

	private Optional<RepositoryFragmentConfiguration> detectRepositoryFragmentConfiguration(String fragmentInterface,
			ImplementationDetectionConfiguration config) {

		ImplementationLookupConfiguration lookup = config.forFragment(fragmentInterface);
		Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(lookup);

		return beanDefinition.map(bd -> new RepositoryFragmentConfiguration(fragmentInterface, bd));
	}

	private void potentiallyRegisterFragmentImplementation(RepositoryConfiguration<?> repositoryConfiguration,
			RepositoryFragmentConfiguration fragmentConfiguration) {

		String beanName = fragmentConfiguration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering repository fragment implementation: %s %s", beanName,
					fragmentConfiguration.getClassName()));
		}

		fragmentConfiguration.getBeanDefinition().ifPresent(bd -> {

			bd.setSource(repositoryConfiguration.getSource());
			registry.registerBeanDefinition(beanName, bd);
		});
	}

	private void potentiallyRegisterRepositoryFragment(RepositoryConfiguration<?> configuration,
			RepositoryFragmentConfiguration fragmentConfiguration) {

		String beanName = fragmentConfiguration.getFragmentBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Registering repository fragment: " + beanName);
		}

		BeanDefinitionBuilder fragmentBuilder = BeanDefinitionBuilder.rootBeanDefinition(RepositoryFragment.class,
				"implemented");

		fragmentBuilder.addConstructorArgValue(fragmentConfiguration.getInterfaceName());
		fragmentBuilder.addConstructorArgReference(fragmentConfiguration.getImplementationBeanName());

		registry.registerBeanDefinition(beanName,
				ParsingUtils.getSourceBeanDefinition(fragmentBuilder, configuration.getSource()));
	}
}
