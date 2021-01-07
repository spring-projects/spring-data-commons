/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * Detects the custom implementation for a {@link org.springframework.data.repository.Repository} instance. If
 * configured with a {@link ImplementationDetectionConfiguration} at construction time, the necessary component scan is
 * executed on first access, cached and its result is the filtered on every further implementation lookup according to
 * the given {@link ImplementationDetectionConfiguration}. If none is given initially, every invocation to
 * {@link #detectCustomImplementation(String, String, ImplementationDetectionConfiguration)} will issue a new component
 * scan.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
 */
public class CustomRepositoryImplementationDetector {

	private static final String CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN = "**/*%s.class";
	private static final String AMBIGUOUS_CUSTOM_IMPLEMENTATIONS = "Ambiguous custom implementations detected! Found %s but expected a single implementation!";

	private final Environment environment;
	private final ResourceLoader resourceLoader;
	private final Lazy<Set<BeanDefinition>> implementationCandidates;

	/**
	 * Creates a new {@link CustomRepositoryImplementationDetector} with the given {@link Environment},
	 * {@link ResourceLoader} and {@link ImplementationDetectionConfiguration}. The latter will be registered for a
	 * one-time component scan for implementation candidates that will the be used and filtered in all subsequent calls to
	 * {@link #detectCustomImplementation(RepositoryConfiguration)}.
	 *
	 * @param environment must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param configuration must not be {@literal null}.
	 */
	public CustomRepositoryImplementationDetector(Environment environment, ResourceLoader resourceLoader,
			ImplementationDetectionConfiguration configuration) {

		Assert.notNull(environment, "Environment must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");
		Assert.notNull(configuration, "ImplementationDetectionConfiguration must not be null!");

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.implementationCandidates = Lazy.of(() -> findCandidateBeanDefinitions(configuration));
	}

	/**
	 * Creates a new {@link CustomRepositoryImplementationDetector} with the given {@link Environment} and
	 * {@link ResourceLoader}. Calls to {@link #detectCustomImplementation(ImplementationLookupConfiguration)} will issue
	 * scans for
	 *
	 * @param environment must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 */
	public CustomRepositoryImplementationDetector(Environment environment, ResourceLoader resourceLoader) {

		Assert.notNull(environment, "Environment must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");

		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.implementationCandidates = Lazy.empty();
	}

	/**
	 * Tries to detect a custom implementation for a repository bean by classpath scanning.
	 *
	 * @param lookup must not be {@literal null}.
	 * @return the {@code AbstractBeanDefinition} of the custom implementation or {@literal null} if none found.
	 */
	public Optional<AbstractBeanDefinition> detectCustomImplementation(ImplementationLookupConfiguration lookup) {

		Assert.notNull(lookup, "ImplementationLookupConfiguration must not be null!");

		Set<BeanDefinition> definitions = implementationCandidates.getOptional()
				.orElseGet(() -> findCandidateBeanDefinitions(lookup)).stream() //
				.filter(lookup::matches) //
				.collect(StreamUtils.toUnmodifiableSet());

		return SelectionSet //
				.of(definitions, c -> c.isEmpty() ? Optional.empty() : throwAmbiguousCustomImplementationException(c)) //
				.filterIfNecessary(lookup::hasMatchingBeanName) //
				.uniqueResult() //
				.map(AbstractBeanDefinition.class::cast);
	}

	private Set<BeanDefinition> findCandidateBeanDefinitions(ImplementationDetectionConfiguration config) {

		String postfix = config.getImplementationPostfix();

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false,
				environment);
		provider.setResourceLoader(resourceLoader);
		provider.setResourcePattern(String.format(CUSTOM_IMPLEMENTATION_RESOURCE_PATTERN, postfix));
		provider.setMetadataReaderFactory(config.getMetadataReaderFactory());
		provider.addIncludeFilter((reader, factory) -> true);

		config.getExcludeFilters().forEach(it -> provider.addExcludeFilter(it));

		return config.getBasePackages().stream()//
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
