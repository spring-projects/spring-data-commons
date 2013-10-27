/*
 * Copyright 2012-2013 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builder to create {@link BeanDefinitionBuilder} instance to eventually create Spring Data repository instances.
 * 
 * @author Oliver Gierke
 */
public class RepositoryBeanDefinitionBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanDefinitionBuilder.class);

	private final RepositoryConfiguration<?> configuration;
	private final RepositoryConfigurationExtension extension;

	/**
	 * Creates a new {@link RepositoryBeanDefinitionBuilder} from the given {@link RepositoryConfiguration} and
	 * {@link RepositoryConfigurationExtension}.
	 * 
	 * @param configuration must not be {@literal null}.
	 * @param extension must not be {@literal null}.
	 */
	public RepositoryBeanDefinitionBuilder(RepositoryConfiguration<?> configuration,
			RepositoryConfigurationExtension extension) {

		Assert.notNull(configuration);
		Assert.notNull(extension);

		this.configuration = configuration;
		this.extension = extension;
	}

	/**
	 * Builds a new {@link BeanDefinitionBuilder} from the given {@link BeanDefinitionRegistry} and {@link ResourceLoader}
	 * .
	 * 
	 * @param registry must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @return
	 */
	public BeanDefinitionBuilder build(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {

		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");

		String factoryBeanName = configuration.getRepositoryFactoryBeanName();
		factoryBeanName = StringUtils.hasText(factoryBeanName) ? factoryBeanName : extension
				.getRepositoryFactoryClassName();

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanName);

		builder.getRawBeanDefinition().setSource(configuration.getSource());
		builder.addPropertyValue("repositoryInterface", configuration.getRepositoryInterface());
		builder.addPropertyValue("queryLookupStrategyKey", configuration.getQueryLookupStrategyKey());
		builder.addPropertyValue("lazyInit", configuration.isLazyInit());

		NamedQueriesBeanDefinitionBuilder definitionBuilder = new NamedQueriesBeanDefinitionBuilder(
				extension.getDefaultNamedQueryLocation());

		if (StringUtils.hasText(configuration.getNamedQueriesLocation())) {
			definitionBuilder.setLocations(configuration.getNamedQueriesLocation());
		}

		builder.addPropertyValue("namedQueries", definitionBuilder.build(configuration.getSource()));

		String customImplementationBeanName = registerCustomImplementation(registry, resourceLoader);

		if (customImplementationBeanName != null) {
			builder.addPropertyReference("customImplementation", customImplementationBeanName);
			builder.addDependsOn(customImplementationBeanName);
		}

		return builder;
	}

	private String registerCustomImplementation(BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {

		String beanName = configuration.getImplementationBeanName();

		// Already a bean configured?
		if (registry.containsBeanDefinition(beanName)) {
			return beanName;
		}

		AbstractBeanDefinition beanDefinition = detectCustomImplementation(registry, resourceLoader);

		if (null == beanDefinition) {
			return null;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Registering custom repository implementation: " + configuration.getImplementationBeanName() + " "
					+ beanDefinition.getBeanClassName());
		}

		beanDefinition.setSource(configuration.getSource());

		registry.registerBeanDefinition(beanName, beanDefinition);

		return beanName;
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 * 
	 * @param config
	 * @param parser
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found
	 */
	private AbstractBeanDefinition detectCustomImplementation(BeanDefinitionRegistry registry, ResourceLoader loader) {

		// Build pattern to lookup implementation class
		Pattern pattern = Pattern.compile(".*\\." + configuration.getImplementationClassName());

		// Build classpath scanner and lookup bean definition
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(loader);
		provider.addIncludeFilter(new RegexPatternTypeFilter(pattern));

		Set<BeanDefinition> definitions = new HashSet<BeanDefinition>();

		for (String basePackage : configuration.getBasePackages()) {
			definitions.addAll(provider.findCandidateComponents(basePackage));
		}

		if (definitions.isEmpty()) {
			return null;
		}

		if (definitions.size() == 1) {
			return (AbstractBeanDefinition) definitions.iterator().next();
		}

		List<String> implementationClassNames = new ArrayList<String>();
		for (BeanDefinition bean : definitions) {
			implementationClassNames.add(bean.getBeanClassName());
		}

		throw new IllegalStateException(String.format(
				"Ambiguous custom implementations detected! Found %s but expected a single implementation!",
				StringUtils.collectionToCommaDelimitedString(implementationClassNames)));
	}
}
