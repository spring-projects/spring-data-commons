/*
 * Copyright 2012-2024 the original author or authors.
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

import static org.springframework.beans.factory.config.BeanDefinition.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsFactoryBean;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
	private final RepositoryFactoriesLoader factoriesLoader;

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

		Assert.notNull(extension, "RepositoryConfigurationExtension must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(environment, "Environment must not be null");

		this.registry = registry;
		this.extension = extension;
		this.resourceLoader = resourceLoader;
		this.factoriesLoader = RepositoryFactoriesLoader.forDefaultResourceLocation(resourceLoader.getClassLoader());
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

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");

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

		String namedQueriesBeanName = BeanDefinitionReaderUtils
				.uniqueBeanName(extension.getModuleIdentifier() + ".named-queries", registry);
		BeanDefinition namedQueries = definitionBuilder.build(configuration.getSource());
		registry.registerBeanDefinition(namedQueriesBeanName, namedQueries);

		builder.addPropertyValue("namedQueries", new RuntimeBeanReference(namedQueriesBeanName));

		registerCustomImplementation(configuration).ifPresent(it -> {
			builder.addPropertyReference("customImplementation", it);
			builder.addDependsOn(it);
		});

		String fragmentsBeanName = registerRepositoryFragments(configuration);
		builder.addPropertyValue("repositoryFragments", new RuntimeBeanReference(fragmentsBeanName));

		return builder;
	}

	// TODO: merge that with the one that creates the BD
	// TODO: Add support for fragments discovered from spring.factories
	RepositoryConfigurationAdapter<?> buildMetadata(RepositoryConfiguration<?> configuration) {

		ImplementationDetectionConfiguration config = configuration
				.toImplementationDetectionConfiguration(metadataReaderFactory);

		List<RepositoryFragmentConfiguration> repositoryFragmentConfigurationStream = fragmentMetadata
				.getFragmentInterfaces(configuration.getRepositoryInterface()) //
				.map(it -> detectRepositoryFragmentConfiguration(it, config, configuration)) //
				.flatMap(Optionals::toStream).toList();

		if (repositoryFragmentConfigurationStream.isEmpty()) {

			ImplementationLookupConfiguration lookup = configuration.toLookupConfiguration(metadataReaderFactory);
			Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(lookup);

			if (beanDefinition.isPresent()) {
				repositoryFragmentConfigurationStream = new ArrayList<>(1);

				List<String> interfaceNames = fragmentMetadata.getFragmentInterfaces(configuration.getRepositoryInterface())
						.toList();
				String implClassName = beanDefinition.get().getBeanClassName();

				try {
					for (String iName : metadataReaderFactory.getMetadataReader(implClassName).getClassMetadata()
							.getInterfaceNames()) {
						if (interfaceNames.contains(iName)) {
							repositoryFragmentConfigurationStream.add(new RepositoryFragmentConfiguration(iName, implClassName));
							break;
						}
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}

		return new RepositoryConfigurationAdapter<>(configuration, repositoryFragmentConfigurationStream);
	}

	private Optional<String> registerCustomImplementation(RepositoryConfiguration<?> configuration) {

		ImplementationLookupConfiguration lookup = configuration.toLookupConfiguration(metadataReaderFactory);

		String configurationBeanName = lookup.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(configurationBeanName)) {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Custom repository implementation already registered: %s", configurationBeanName));
			}

			return Optional.of(configurationBeanName);
		}

		return implementationDetector.detectCustomImplementation(lookup)
				.map(it -> potentiallyRegisterRepositoryImplementation(configuration, it));
	}

	private String registerRepositoryFragments(RepositoryConfiguration<?> configuration) {

		BeanDefinitionBuilder fragmentsBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(RepositoryFragmentsFactoryBean.class) //
				.setRole(ROLE_INFRASTRUCTURE);

		List<String> fragmentBeanNames = registerRepositoryFragmentsImplementation(configuration) //
				.map(RepositoryFragmentConfiguration::getFragmentBeanName) //
				.collect(Collectors.toList());

		fragmentsBuilder.addConstructorArgValue(fragmentBeanNames);

		String fragmentsBeanName = BeanDefinitionReaderUtils
				.uniqueBeanName(String.format("%s.%s.fragments", extension.getModuleName().toLowerCase(Locale.ROOT),
						ClassUtils.getShortName(configuration.getRepositoryInterface())), registry);
		registry.registerBeanDefinition(fragmentsBeanName, fragmentsBuilder.getBeanDefinition());
		return fragmentsBeanName;
	}

	private Stream<RepositoryFragmentConfiguration> registerRepositoryFragmentsImplementation(
			RepositoryConfiguration<?> configuration) {

		ImplementationDetectionConfiguration config = configuration
				.toImplementationDetectionConfiguration(metadataReaderFactory);

		Stream<RepositoryFragmentConfiguration> discovered = discoverFragments(configuration, config);
		Stream<RepositoryFragmentConfiguration> loaded = loadFragments(configuration);

		return Stream.concat(discovered, loaded) //
				.peek(it -> potentiallyRegisterFragmentImplementation(configuration, it)) //
				.peek(it -> potentiallyRegisterRepositoryFragment(configuration, it));
	}

	private Stream<RepositoryFragmentConfiguration> discoverFragments(RepositoryConfiguration<?> configuration,
			ImplementationDetectionConfiguration config) {
		return fragmentMetadata.getFragmentInterfaces(configuration.getRepositoryInterface())
				.map(it -> detectRepositoryFragmentConfiguration(it, config, configuration)) //
				.flatMap(Optionals::toStream);
	}

	private Stream<RepositoryFragmentConfiguration> loadFragments(RepositoryConfiguration<?> configuration) {

		List<String> names = factoriesLoader.loadFactoryNames(configuration.getRepositoryInterface());

		if (names.isEmpty()) {
			return Stream.empty();
		}

		return names.stream().map(it -> createFragmentConfiguration(null, configuration, it));
	}

	private Optional<RepositoryFragmentConfiguration> detectRepositoryFragmentConfiguration(String fragmentInterface,
			ImplementationDetectionConfiguration config, RepositoryConfiguration<?> configuration) {

		List<String> names = factoriesLoader.loadFactoryNames(fragmentInterface);

		if (names.isEmpty()) {

			ImplementationLookupConfiguration lookup = config.forFragment(fragmentInterface);
			Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(lookup);

			return beanDefinition.map(bd -> createFragmentConfiguration(fragmentInterface, configuration, bd));
		}

		if (names.size() > 1) {
			logger.debug(String.format("Multiple fragment implementations %s registered for fragment interface %s", names,
					fragmentInterface));
		}

		return Optional.of(createFragmentConfiguration(fragmentInterface, configuration, names.get(0)));
	}

	private RepositoryFragmentConfiguration createFragmentConfiguration(@Nullable String fragmentInterface,
			RepositoryConfiguration<?> configuration, String className) {

		try {

			MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
			AnnotatedGenericBeanDefinition bd = new AnnotatedGenericBeanDefinition(metadataReader.getAnnotationMetadata());
			return createFragmentConfiguration(fragmentInterface, configuration, bd);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static RepositoryFragmentConfiguration createFragmentConfiguration(@Nullable String fragmentInterface,
			RepositoryConfiguration<?> configuration, AbstractBeanDefinition beanDefinition) {

		return new RepositoryFragmentConfiguration(fragmentInterface, beanDefinition,
				configuration.getConfigurationSource().generateBeanName(beanDefinition));
	}

	private String potentiallyRegisterRepositoryImplementation(RepositoryConfiguration<?> configuration,
			AbstractBeanDefinition beanDefinition) {

		String targetBeanName = configuration.getConfigurationSource().generateBeanName(beanDefinition);
		beanDefinition.setSource(configuration.getSource());

		if (registry.containsBeanDefinition(targetBeanName)) {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Custom repository implementation already registered: %s %s", targetBeanName,
						beanDefinition.getBeanClassName()));
			}
		} else {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Registering custom repository implementation: %s %s", targetBeanName,
						beanDefinition.getBeanClassName()));
			}

			registry.registerBeanDefinition(targetBeanName, beanDefinition);
		}

		return targetBeanName;
	}

	private void potentiallyRegisterFragmentImplementation(RepositoryConfiguration<?> repositoryConfiguration,
			RepositoryFragmentConfiguration fragmentConfiguration) {

		String beanName = fragmentConfiguration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Repository fragment implementation already registered: %s", beanName));
			}

			return;
		}

		fragmentConfiguration.getBeanDefinition().ifPresent(bd -> {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Registering repository fragment implementation: %s %s", beanName,
						fragmentConfiguration.getClassName()));
			}

			bd.setSource(repositoryConfiguration.getSource());
			registry.registerBeanDefinition(beanName, bd);
		});
	}

	private void potentiallyRegisterRepositoryFragment(RepositoryConfiguration<?> configuration,
			RepositoryFragmentConfiguration fragmentConfiguration) {

		String beanName = fragmentConfiguration.getFragmentBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("RepositoryFragment already registered: %s", beanName));
			}

			return;
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering RepositoryFragment: %s", beanName));
		}

		BeanDefinitionBuilder fragmentBuilder = BeanDefinitionBuilder.rootBeanDefinition(RepositoryFragment.class,
				"implemented");

		if (StringUtils.hasText(fragmentConfiguration.getInterfaceName())) {
			fragmentBuilder.addConstructorArgValue(fragmentConfiguration.getInterfaceName());
		}
		fragmentBuilder.addConstructorArgReference(fragmentConfiguration.getImplementationBeanName());

		registry.registerBeanDefinition(beanName,
				ParsingUtils.getSourceBeanDefinition(fragmentBuilder, configuration.getSource()));
	}

	static class RepositoryFactoriesLoader extends SpringFactoriesLoader {

		private final Map<String, List<String>> factories;

		/**
		 * Create a new {@link SpringFactoriesLoader} instance.
		 *
		 * @param classLoader the classloader used to instantiate the factories
		 * @param factories a map of factory class name to implementation class names
		 */
		protected RepositoryFactoriesLoader(@Nullable ClassLoader classLoader, Map<String, List<String>> factories) {
			super(classLoader, factories);
			this.factories = factories;
		}

		/**
		 * Create a {@link RepositoryFactoriesLoader} instance that will load and instantiate the factory implementations
		 * from the default location, using the given class loader.
		 *
		 * @param classLoader the ClassLoader to use for loading resources; can be {@code null} to use the default
		 * @return a {@link RepositoryFactoriesLoader} instance
		 * @see #forResourceLocation(String)
		 */
		public static RepositoryFactoriesLoader forDefaultResourceLocation(@Nullable ClassLoader classLoader) {
			ClassLoader resourceClassLoader = (classLoader != null ? classLoader
					: SpringFactoriesLoader.class.getClassLoader());
			return new RepositoryFactoriesLoader(classLoader,
					loadFactoriesResource(resourceClassLoader, FACTORIES_RESOURCE_LOCATION));
		}

		List<String> loadFactoryNames(String factoryType) {
			return this.factories.getOrDefault(factoryType, Collections.emptyList());
		}
	}
}
