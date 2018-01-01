/*
 * Copyright 2014-2018 the original author or authors.
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Detects the custom implementation for a {@link org.springframework.data.repository.Repository}
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
 */
@RequiredArgsConstructor
public class CustomRepositoryImplementationDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";
	private static final String AMBIGUOUS_CUSTOM_IMPLEMENTATIONS = "Ambiguous custom implementations detected! Found %s but expected a single implementation!";

	private final @NonNull MetadataReaderFactory metadataReaderFactory;
	private final @NonNull Environment environment;
	private final @NonNull ResourceLoader resourceLoader;

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param configuration the {@link RepositoryConfiguration} to consider.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	@SuppressWarnings("deprecation")
	public Optional<AbstractBeanDefinition> detectCustomImplementation(RepositoryConfiguration<?> configuration) {

		// TODO 2.0: Extract into dedicated interface for custom implementation lookup configuration.

		return detectCustomImplementation( //
				configuration.getImplementationClassName(), //
				configuration.getImplementationBeanName(), //
				configuration.getImplementationBasePackages(), //
				configuration.getExcludeFilters(), //
				bd -> configuration.getConfigurationSource().generateBeanName(bd));
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param className must not be {@literal null}.
	 * @param beanName may be {@literal null}
	 * @param basePackages must not be {@literal null}.
	 * @param excludeFilters must not be {@literal null}.
	 * @param beanNameGenerator must not be {@literal null}.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public Optional<AbstractBeanDefinition> detectCustomImplementation(String className, @Nullable String beanName,
			Iterable<String> basePackages, Iterable<TypeFilter> excludeFilters,
			Function<BeanDefinition, String> beanNameGenerator) {

		Assert.notNull(className, "ClassName must not be null!");
		Assert.notNull(basePackages, "BasePackages must not be null!");

		Set<BeanDefinition> definitions = findCandidateBeanDefinitions(className, basePackages, excludeFilters);

		return SelectionSet //
				.of(definitions, c -> c.isEmpty() ? Optional.empty() : throwAmbiguousCustomImplementationException(c)) //
				.filterIfNecessary(bd -> beanName != null && beanName.equals(beanNameGenerator.apply(bd)))//
				.uniqueResult().map(it -> AbstractBeanDefinition.class.cast(it));
	}

	Set<BeanDefinition> findCandidateBeanDefinitions(String className, Iterable<String> basePackages,
			Iterable<TypeFilter> excludeFilters) {

		// Build pattern to lookup implementation class
		Pattern pattern = Pattern.compile(".*\\." + className);

		// Build classpath scanner and lookup bean definition
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false,
				environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(String.format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, className));
		provider.setMetadataReaderFactory(metadataReaderFactory);
		provider.addIncludeFilter(new RegexPatternTypeFilter(pattern));

		excludeFilters.forEach(it -> provider.addExcludeFilter(it));

		return Streamable.of(basePackages).stream()//
				.flatMap(it -> provider.findCandidateComponents(it).stream())//
				.collect(Collectors.toSet());
	}

	private static Optional<BeanDefinition> throwAmbiguousCustomImplementationException(
			Collection<BeanDefinition> definitions) {

		String implementationNames = definitions.stream()//
				.map(BeanDefinition::getBeanClassName)//
				.collect(Collectors.joining(", "));

		throw new IllegalStateException(String.format(AMBIGUOUS_CUSTOM_IMPLEMENTATIONS, implementationNames));
	}
}
