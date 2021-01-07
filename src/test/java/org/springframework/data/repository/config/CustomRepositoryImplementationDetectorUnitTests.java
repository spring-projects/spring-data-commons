/*
 * Copyright 2017-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetectorUnitTests.First.CanonicalSampleRepositoryTestImpl;
import org.springframework.data.util.Streamable;
import org.springframework.mock.env.MockEnvironment;

/**
 * tests {@link CustomRepositoryImplementationDetector}
 *
 * @author Jens Schauder
 */
class CustomRepositoryImplementationDetectorUnitTests {

	MetadataReaderFactory metadataFactory = new SimpleMetadataReaderFactory();
	Environment environment = new MockEnvironment();
	ResourceLoader resourceLoader = new DefaultResourceLoader();
	ImplementationDetectionConfiguration configuration = mock(ImplementationDetectionConfiguration.class,
			Answers.RETURNS_MOCKS);

	CustomRepositoryImplementationDetector detector = new CustomRepositoryImplementationDetector(environment,
			resourceLoader, configuration);

	{
		when(configuration.forRepositoryConfiguration(any(RepositoryConfiguration.class))).thenCallRealMethod();
		when(configuration.getMetadataReaderFactory()).thenReturn(metadataFactory);
		when(configuration.getBasePackages()).thenReturn(Streamable.of(this.getClass().getPackage().getName()));
		when(configuration.getImplementationPostfix()).thenReturn("TestImpl");
	}

	@Test // DATACMNS-764, DATACMNS-1371
	void returnsNullWhenNoImplementationFound() {

		RepositoryConfiguration mock = mock(RepositoryConfiguration.class);

		ImplementationLookupConfiguration lookup = configuration
				.forRepositoryConfiguration(configFor(NoImplementationRepository.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation(lookup);

		assertThat(beanDefinition).isEmpty();
	}

	@Test // DATACMNS-764, DATACMNS-1371
	void returnsBeanDefinitionWhenOneImplementationIsFound() {

		ImplementationLookupConfiguration lookup = configuration
				.forRepositoryConfiguration(configFor(SingleSampleRepository.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation(lookup);

		assertThat(beanDefinition).hasValueSatisfying(
				it -> assertThat(it.getBeanClassName()).isEqualTo(SingleSampleRepositoryTestImpl.class.getName()));
	}

	@Test // DATACMNS-764, DATACMNS-1371
	void returnsBeanDefinitionMatchingByNameWhenMultipleImplementationAreFound() {

		when(configuration.generateBeanName(any())).then(it -> {

			BeanDefinition definition = it.getArgument(0);
			String className = definition.getBeanClassName();

			return className.contains("$First$") ? "canonicalSampleRepositoryTestImpl" : "otherBeanName";
		});

		ImplementationLookupConfiguration lookup = configuration
				.forRepositoryConfiguration(configFor(CanonicalSampleRepository.class));

		assertThat(detector.detectCustomImplementation(lookup)) //
				.hasValueSatisfying(
						it -> assertThat(it.getBeanClassName()).isEqualTo(CanonicalSampleRepositoryTestImpl.class.getName()));
	}

	@Test // DATACMNS-764, DATACMNS-1371
	void throwsExceptionWhenMultipleImplementationAreFound() {

		assertThatIllegalStateException().isThrownBy(() -> {

			ImplementationLookupConfiguration lookup = mock(ImplementationLookupConfiguration.class);

			when(lookup.hasMatchingBeanName(any())).thenReturn(true);
			when(lookup.matches(any())).thenReturn(true);

			detector.detectCustomImplementation(lookup);
		});
	}

	private RepositoryConfiguration configFor(Class<?> type) {

		RepositoryConfiguration<?> configuration = mock(RepositoryConfiguration.class);

		when(configuration.getRepositoryInterface()).thenReturn(type.getSimpleName());
		when(configuration.getImplementationBasePackages())
				.thenReturn(Streamable.of(this.getClass().getPackage().getName()));

		return configuration;
	}

	// No implementation

	interface NoImplementationRepository {}

	// Single implementation

	interface SingleSampleRepository {}

	static class SingleSampleRepositoryTestImpl implements SingleSampleRepository {}

	// Multiple implementations

	interface CanonicalSampleRepository {}

	static class First {
		static class CanonicalSampleRepositoryTestImpl implements CanonicalSampleRepository {}
	}

	static class Second {
		static class CanonicalSampleRepositoryTestImpl implements CanonicalSampleRepository {}
	}
}
