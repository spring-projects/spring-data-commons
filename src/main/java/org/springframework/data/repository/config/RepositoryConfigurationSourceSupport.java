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

import java.util.Collections;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Base class to implement {@link RepositoryConfigurationSource}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Peter Rietzler
 * @author Jens Schauder
 */
public abstract class RepositoryConfigurationSourceSupport implements RepositoryConfigurationSource {

	protected static final String DEFAULT_REPOSITORY_IMPL_POSTFIX = "Impl";

	private final Environment environment;
	private final RepositoryBeanNameGenerator beanNameGenerator;
	private final BeanDefinitionRegistry registry;

	/**
	 * Creates a new {@link RepositoryConfigurationSourceSupport} with the given environment.
	 *
	 * @param environment must not be {@literal null}.
	 * @param classLoader must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 */
	public RepositoryConfigurationSourceSupport(Environment environment, ClassLoader classLoader,
			BeanDefinitionRegistry registry, BeanNameGenerator generator) {

		Assert.notNull(environment, "Environment must not be null!");
		Assert.notNull(classLoader, "ClassLoader must not be null!");
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null!");

		this.environment = environment;
		this.beanNameGenerator = new RepositoryBeanNameGenerator(classLoader, generator, registry);
		this.registry = registry;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getCandidates(org.springframework.core.io.ResourceLoader)
	 */
	@Override
	public Streamable<BeanDefinition> getCandidates(ResourceLoader loader) {

		RepositoryComponentProvider scanner = new RepositoryComponentProvider(getIncludeFilters(), registry);
		scanner.setConsiderNestedRepositoryInterfaces(shouldConsiderNestedRepositories());
		scanner.setEnvironment(environment);
		scanner.setResourceLoader(loader);

		getExcludeFilters().forEach(it -> scanner.addExcludeFilter(it));

		return Streamable.of(() -> getBasePackages().stream()//
				.flatMap(it -> scanner.findCandidateComponents(it).stream()));
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to exclude when scanning for repositories. Default
	 * implementation returns an empty collection.
	 *
	 * @return must not be {@literal null}.
	 */
	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return Streamable.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getBeanNameGenerator()
	 */
	@Override
	public String generateBeanName(BeanDefinition beanDefinition) {
		return beanNameGenerator.generateBeanName(beanDefinition);
	}

	/**
	 * Return the {@link TypeFilter}s to define which types to include when scanning for repositories. Default
	 * implementation returns an empty collection.
	 *
	 * @return must not be {@literal null}.
	 */
	protected Iterable<TypeFilter> getIncludeFilters() {
		return Collections.emptySet();
	}

	/**
	 * Returns whether we should consider nested repositories, i.e. repository interface definitions nested in other
	 * classes.
	 *
	 * @return {@literal true} if the container should look for nested repository interface definitions.
	 */
	public boolean shouldConsiderNestedRepositories() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#toImplementationDetectionConfiguration()
	 */
	@Override
	public ImplementationDetectionConfiguration toImplementationDetectionConfiguration(MetadataReaderFactory factory) {
		return new SpringImplementationDetectionConfiguration(this, factory);
	}

	private class SpringImplementationDetectionConfiguration implements ImplementationDetectionConfiguration {

		private final RepositoryConfigurationSource source;
		private final MetadataReaderFactory metadataReaderFactory;

		SpringImplementationDetectionConfiguration(RepositoryConfigurationSource source,
				MetadataReaderFactory metadataReaderFactory) {
			this.source = source;
			this.metadataReaderFactory = metadataReaderFactory;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getImplementationPostfix()
		 */
		@Override
		public String getImplementationPostfix() {
			return source.getRepositoryImplementationPostfix()
					.orElse(DefaultRepositoryConfiguration.DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getBasePackages()
		 */
		@Override
		public Streamable<String> getBasePackages() {
			return source.getBasePackages();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getExcludeFilters()
		 */
		@Override
		public Streamable<TypeFilter> getExcludeFilters() {
			return source.getExcludeFilters();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#generateBeanName(org.springframework.beans.factory.config.BeanDefinition)
		 */
		@Override
		public String generateBeanName(BeanDefinition definition) {
			return source.generateBeanName(definition);
		}

		public MetadataReaderFactory getMetadataReaderFactory() {
			return this.metadataReaderFactory;
		}
	}
}
