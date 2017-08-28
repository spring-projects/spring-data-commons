/*
 * Copyright 2012-2017 the original author or authors.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsFactoryBean;
import org.springframework.data.repository.query.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder to create {@link BeanDefinitionBuilder} instance to eventually create Spring Data repository instances.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Mark Paluch
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

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(configuration.getRepositoryFactoryBeanClassName());

		builder.getRawBeanDefinition().setSource(configuration.getSource());
		builder.addConstructorArgValue(configuration.getRepositoryInterface());
		builder.addPropertyValue("queryLookupStrategyKey", configuration.getQueryLookupStrategyKey());
		builder.addPropertyValue("lazyInit", configuration.isLazyInit());

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
				.stream() //
				.map(RepositoryFragmentConfiguration::getFragmentBeanName) //
				.collect(Collectors.toList());

		fragmentsBuilder.addConstructorArgValue(fragmentBeanNames);

		builder.addPropertyValue("repositoryFragments",
				ParsingUtils.getSourceBeanDefinition(fragmentsBuilder, configuration.getSource()));

		RootBeanDefinition evaluationContextProviderDefinition = new RootBeanDefinition(
				ExtensionAwareEvaluationContextProvider.class);
		evaluationContextProviderDefinition.setSource(configuration.getSource());

		builder.addPropertyValue("evaluationContextProvider", evaluationContextProviderDefinition);

		return builder;
	}

	@SuppressWarnings("deprecation")
	private Optional<String> registerCustomImplementation(RepositoryConfiguration<?> configuration) {

		String beanName = configuration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return Optional.of(beanName);
		}

		Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(configuration);

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

	private List<RepositoryFragmentConfiguration> registerRepositoryFragmentsImplementation(
			RepositoryConfiguration<?> configuration) {

		ClassMetadata classMetadata = getClassMetadata(configuration.getRepositoryInterface());

		return Arrays.stream(classMetadata.getInterfaceNames()) //
				.filter(this::isFragmentInterfaceCandidate) //
				.map(it -> detectRepositoryFragmentConfiguration(configuration, it)) //
				.filter(Optional::isPresent) //
				.map(Optional::get) //
				.peek(it -> potentiallyRegisterFragmentImplementation(configuration, it)) //
				.peek(it -> potentiallyRegisterRepositoryFragment(configuration, it)) //
				.collect(Collectors.toList());
	}

	private boolean isFragmentInterfaceCandidate(String interfaceName) {

		AnnotationMetadata metadata = getAnnotationMetadata(interfaceName);

		return !metadata.hasAnnotation(NoRepositoryBean.class.getName());
	}

	private Optional<RepositoryFragmentConfiguration> detectRepositoryFragmentConfiguration(
			RepositoryConfiguration<?> configuration, String fragmentInterfaceName) {

		List<TypeFilter> exclusions = getExclusions(configuration);

		String className = ClassUtils.getShortName(fragmentInterfaceName)
				.concat(configuration.getConfigurationSource().getRepositoryImplementationPostfix().orElse("Impl"));

		Optional<AbstractBeanDefinition> beanDefinition = implementationDetector.detectCustomImplementation(className, null,
				configuration.getBasePackages(), exclusions, bd -> configuration.getConfigurationSource().generateBeanName(bd));

		return beanDefinition.map(bd -> new RepositoryFragmentConfiguration(fragmentInterfaceName, bd));
	}

	private void potentiallyRegisterFragmentImplementation(RepositoryConfiguration<?> repositoryConfiguration,
			RepositoryFragmentConfiguration fragmentConfiguration) {

		String beanName = fragmentConfiguration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Registering repository fragment implementation: %s %s", beanName,
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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Registering repository fragment: " + beanName);
		}

		BeanDefinitionBuilder fragmentBuilder = BeanDefinitionBuilder.rootBeanDefinition(RepositoryFragment.class,
				"implemented");

		fragmentBuilder.addConstructorArgValue(fragmentConfiguration.getInterfaceName());
		fragmentBuilder.addConstructorArgReference(fragmentConfiguration.getImplementationBeanName());

		registry.registerBeanDefinition(beanName,
				ParsingUtils.getSourceBeanDefinition(fragmentBuilder, configuration.getSource()));
	}

	private ClassMetadata getClassMetadata(String className) {

		try {
			return metadataReaderFactory.getMetadataReader(className).getClassMetadata();
		} catch (IOException e) {
			throw new BeanDefinitionStoreException(String.format("Cannot parse %s metadata.", className), e);
		}
	}

	private AnnotationMetadata getAnnotationMetadata(String className) {

		try {
			return metadataReaderFactory.getMetadataReader(className).getAnnotationMetadata();
		} catch (IOException e) {
			throw new BeanDefinitionStoreException(String.format("Cannot parse %s metadata.", className), e);
		}
	}

	private static List<TypeFilter> getExclusions(RepositoryConfiguration<?> configuration) {

		return Stream
				.concat(configuration.getExcludeFilters().stream(), Stream.of(new AnnotationTypeFilter(NoRepositoryBean.class)))//
				.collect(StreamUtils.toUnmodifiableList());
	}
}
