/*
 * Copyright 2017 the original author or authors.
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

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.mock.env.MockEnvironment;

/**
 * tests {@link CustomRepositoryImplementationDetector}
 *
 * @author Jens Schauder
 */
public class CustomRepositoryImplementationDetectorUnitTests {

	MetadataReaderFactory metadataFactory = new SimpleMetadataReaderFactory();
	Environment environment = new MockEnvironment();
	ResourceLoader resourceLoader = new DefaultResourceLoader();
	Function<BeanDefinition, String> nameGenerator = mock(Function.class);

	CustomRepositoryImplementationDetector detector = spy(
			new CustomRepositoryImplementationDetector(metadataFactory, environment, resourceLoader));

	{
		doReturn("notTheBeanYouAreLookingFor").when(nameGenerator).apply(any(BeanDefinition.class));
	}

	@Test // DATACMNS-764
	public void returnsNullWhenNoImplementationFound() {

		doReturn(emptySet()).when(detector).findCandidateBeanDefinitions(anyString(), anyListOf(String.class),
				anyListOf(TypeFilter.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation("className", "beanName", emptyList(),
				emptyList(), nameGenerator);

		assertThat(beanDefinition).isEmpty();
	}

	@Test // DATACMNS-764
	public void returnsBeanDefinitionWhenOneImplementationIsFound() {

		AbstractBeanDefinition expectedBeanDefinition = mock(AbstractBeanDefinition.class);

		doReturn(new HashSet<>(singleton(expectedBeanDefinition))).when(detector).findCandidateBeanDefinitions(anyString(),
				anyListOf(String.class), anyListOf(TypeFilter.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation("className", "beanName", emptyList(),
				emptyList(), nameGenerator);

		assertThat(beanDefinition).contains(expectedBeanDefinition);
	}

	@Test // DATACMNS-764
	public void returnsBeanDefinitionMatchingByNameWhenMultipleImplementationAreFound() {

		AbstractBeanDefinition wrongBeanDefinition = mock(AbstractBeanDefinition.class);
		AbstractBeanDefinition expectedBeanDefinition = mock(AbstractBeanDefinition.class);

		doReturn("expected").when(nameGenerator).apply(expectedBeanDefinition);

		doReturn(new HashSet<>(asList(wrongBeanDefinition, expectedBeanDefinition))).when(detector)
				.findCandidateBeanDefinitions(anyString(), anyListOf(String.class), anyListOf(TypeFilter.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation("className", "expected", emptyList(),
				emptyList(), nameGenerator);

		assertThat( beanDefinition).contains(expectedBeanDefinition);
	}

	@Test(expected = IllegalStateException.class) // DATACMNS-764
	public void throwsExceptionWhenMultipleImplementationAreFound() {

		AbstractBeanDefinition wrongBeanDefinition = mock(AbstractBeanDefinition.class);
		AbstractBeanDefinition expectedBeanDefinition = mock(AbstractBeanDefinition.class);

		doReturn("expected").when(nameGenerator).apply(any(BeanDefinition.class));

		doReturn(new HashSet<>(asList(wrongBeanDefinition, expectedBeanDefinition))).when(detector)
				.findCandidateBeanDefinitions(anyString(), anyListOf(String.class), anyListOf(TypeFilter.class));

		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation("className", "expected", emptyList(),
				emptyList(), nameGenerator);
	}
}
