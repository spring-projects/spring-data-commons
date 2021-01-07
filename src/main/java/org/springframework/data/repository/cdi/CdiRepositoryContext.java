/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.repository.cdi;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.inject.UnsatisfiedResolutionException;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.FragmentMetadata;
import org.springframework.data.repository.config.ImplementationDetectionConfiguration;
import org.springframework.data.repository.config.ImplementationLookupConfiguration;
import org.springframework.data.repository.config.RepositoryFragmentConfiguration;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Context for CDI repositories. This class provides {@link ClassLoader} and
 * {@link org.springframework.data.repository.core.support.RepositoryFragment detection} which are commonly used within
 * CDI.
 *
 * @author Mark Paluch
 * @since 2.1
 */
public class CdiRepositoryContext {

	private final ClassLoader classLoader;
	private final CustomRepositoryImplementationDetector detector;
	private final MetadataReaderFactory metadataReaderFactory;
	private final FragmentMetadata metdata;

	/**
	 * Create a new {@link CdiRepositoryContext} given {@link ClassLoader} and initialize
	 * {@link CachingMetadataReaderFactory}.
	 *
	 * @param classLoader must not be {@literal null}.
	 */
	public CdiRepositoryContext(ClassLoader classLoader) {
		this(classLoader, new CustomRepositoryImplementationDetector(new StandardEnvironment(),
				new PathMatchingResourcePatternResolver(classLoader)));
	}

	/**
	 * Create a new {@link CdiRepositoryContext} given {@link ClassLoader} and
	 * {@link CustomRepositoryImplementationDetector}.
	 *
	 * @param classLoader must not be {@literal null}.
	 * @param detector must not be {@literal null}.
	 */
	public CdiRepositoryContext(ClassLoader classLoader, CustomRepositoryImplementationDetector detector) {

		Assert.notNull(classLoader, "ClassLoader must not be null!");
		Assert.notNull(detector, "CustomRepositoryImplementationDetector must not be null!");

		ResourceLoader resourceLoader = new PathMatchingResourcePatternResolver(classLoader);

		this.classLoader = classLoader;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
		this.metdata = new FragmentMetadata(metadataReaderFactory);
		this.detector = detector;
	}

	CustomRepositoryImplementationDetector getCustomRepositoryImplementationDetector() {
		return detector;
	}

	/**
	 * Load a {@link Class} using the CDI {@link ClassLoader}.
	 *
	 * @param className
	 * @return
	 * @throws UnsatisfiedResolutionException if the class cannot be found.
	 */
	Class<?> loadClass(String className) {

		try {
			return ClassUtils.forName(className, classLoader);
		} catch (ClassNotFoundException e) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve class for '%s'", className), e);
		}
	}

	/**
	 * Discover {@link RepositoryFragmentConfiguration fragment configurations} for a {@link Class repository interface}.
	 *
	 * @param configuration must not be {@literal null}.
	 * @param repositoryInterface must not be {@literal null}.
	 * @return {@link Stream} of {@link RepositoryFragmentConfiguration fragment configurations}.
	 */
	Stream<RepositoryFragmentConfiguration> getRepositoryFragments(CdiRepositoryConfiguration configuration,
			Class<?> repositoryInterface) {

		CdiImplementationDetectionConfiguration config = new CdiImplementationDetectionConfiguration(configuration,
				metadataReaderFactory);

		return metdata.getFragmentInterfaces(repositoryInterface.getName()) //
				.map(it -> detectRepositoryFragmentConfiguration(it, config)) //
				.flatMap(Optionals::toStream);
	}

	/**
	 * Retrieves a custom repository interfaces from a repository type. This works for the whole class hierarchy and can
	 * find also a custom repository which is inherited over many levels.
	 *
	 * @param repositoryType The class representing the repository.
	 * @param cdiRepositoryConfiguration The configuration for CDI usage.
	 * @return the interface class or {@literal null}.
	 */
	Optional<Class<?>> getCustomImplementationClass(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		ImplementationDetectionConfiguration configuration = new CdiImplementationDetectionConfiguration(
				cdiRepositoryConfiguration, metadataReaderFactory);
		ImplementationLookupConfiguration lookup = configuration.forFragment(repositoryType.getName());

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation(lookup);

		return beanDefinition.map(this::loadBeanClass);
	}

	private Optional<RepositoryFragmentConfiguration> detectRepositoryFragmentConfiguration(String fragmentInterfaceName,
			CdiImplementationDetectionConfiguration config) {

		ImplementationLookupConfiguration lookup = config.forFragment(fragmentInterfaceName);
		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation(lookup);

		return beanDefinition.map(bd -> new RepositoryFragmentConfiguration(fragmentInterfaceName, bd));
	}

	@Nullable
	private Class<?> loadBeanClass(AbstractBeanDefinition definition) {

		String beanClassName = definition.getBeanClassName();

		return beanClassName == null ? null : loadClass(beanClassName);
	}

	private static class CdiImplementationDetectionConfiguration implements ImplementationDetectionConfiguration {

		private final CdiRepositoryConfiguration configuration;
		private final MetadataReaderFactory metadataReaderFactory;

		CdiImplementationDetectionConfiguration(CdiRepositoryConfiguration configuration,
				MetadataReaderFactory metadataReaderFactory) {

			this.configuration = configuration;
			this.metadataReaderFactory = metadataReaderFactory;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getImplementationPostfix()
		 */
		@Override
		public String getImplementationPostfix() {
			return configuration.getRepositoryImplementationPostfix();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getBasePackages()
		 */
		@Override
		public Streamable<String> getBasePackages() {
			return Streamable.empty();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.CustomRepositoryImplementationDetector.ImplementationDetectionConfiguration#getExcludeFilters()
		 */
		@Override
		public Streamable<TypeFilter> getExcludeFilters() {
			return Streamable.empty();
		}

		public MetadataReaderFactory getMetadataReaderFactory() {
			return this.metadataReaderFactory;
		}
	}
}
