/*
 * Copyright 2014-2017 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Detects the custom implementation for a {@link org.springframework.data.repository.Repository}
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Peter Rietzler
 * @author Mark Paluch
 */
public class CustomRepositoryImplementationDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";

	private final MetadataReaderFactory metadataReaderFactory;
	private final Environment environment;
	private final ResourceLoader resourceLoader;

	/**
	 * Creates a new {@link CustomRepositoryImplementationDetector} from the given
	 * {@link org.springframework.core.type.classreading.MetadataReaderFactory},
	 * {@link org.springframework.core.env.Environment} and {@link org.springframework.core.io.ResourceLoader}.
	 *
	 * @param metadataReaderFactory must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 */
	public CustomRepositoryImplementationDetector(MetadataReaderFactory metadataReaderFactory, Environment environment,
			ResourceLoader resourceLoader) {

		Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		Assert.notNull(environment, "Environment must not be null!");

		this.metadataReaderFactory = metadataReaderFactory;
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 * 
	 * @param configuration the {@link RepositoryConfiguration} to consider.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public AbstractBeanDefinition detectCustomImplementation(RepositoryConfiguration<?> configuration) {

		// TODO 2.0: Extract into dedicated interface for custom implementation lookup configuration.

		return detectCustomImplementation(configuration.getImplementationClassName(), //
				configuration.getImplementationBasePackages(), //
				configuration.getExcludeFilters());
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 * 
	 * @param className must not be {@literal null}.
	 * @param basePackages must not be {@literal null}.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public AbstractBeanDefinition detectCustomImplementation(String className, Iterable<String> basePackages,
			Iterable<TypeFilter> excludeFilters) {

		Assert.notNull(className, "ClassName must not be null!");
		Assert.notNull(basePackages, "BasePackages must not be null!");

		// Build pattern to lookup implementation class
		Pattern pattern = Pattern.compile(".*\\." + className);

		// Build classpath scanner and lookup bean definition
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false,
				environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(String.format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, className));
		provider.setMetadataReaderFactory(metadataReaderFactory);
		provider.addIncludeFilter(new RegexPatternTypeFilter(pattern));

		for (TypeFilter excludeFilter : excludeFilters) {
			provider.addExcludeFilter(excludeFilter);
		}

		Set<BeanDefinition> definitions = new HashSet<BeanDefinition>();

		for (String basePackage : basePackages) {
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

		throw new IllegalStateException(
				String.format("Ambiguous custom implementations detected! Found %s but expected a single implementation!",
						StringUtils.collectionToCommaDelimitedString(implementationClassNames)));
	}
}
