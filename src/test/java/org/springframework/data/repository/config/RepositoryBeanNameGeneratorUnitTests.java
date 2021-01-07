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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * Unit tests for {@link RepositoryBeanNameGenerator}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 * @soundtrack Take Me Away (Extended Mix) - Kaimo K, Trance Classics & Susanne Teutenberg
 */
class RepositoryBeanNameGeneratorUnitTests {

	static final String SAMPLE_IMPLEMENTATION_BEAN_NAME = "repositoryBeanNameGeneratorUnitTests.SomeImplementation";

	RepositoryBeanNameGenerator generator;
	BeanDefinitionRegistry registry;

	@BeforeEach
	void setUp() {
		this.generator = new RepositoryBeanNameGenerator(Thread.currentThread().getContextClassLoader(),
				new AnnotationBeanNameGenerator(), new SimpleBeanDefinitionRegistry());
	}

	@Test
	void usesPlainClassNameIfNoAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(MyRepository.class))).isEqualTo("myRepository");
	}

	@Test
	void usesAnnotationValueIfAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(AnnotatedInterface.class))).isEqualTo("specialName");
	}

	@Test // DATACMNS-1115
	void usesClassNameOfScannedBeanDefinition() throws IOException {

		MetadataReaderFactory factory = new SimpleMetadataReaderFactory();
		MetadataReader reader = factory.getMetadataReader(SomeImplementation.class.getName());

		BeanDefinition definition = new ScannedGenericBeanDefinition(reader);

		assertThat(generator.generateBeanName(definition)).isEqualTo(SAMPLE_IMPLEMENTATION_BEAN_NAME);
	}

	@Test // DATACMNS-1115
	void usesClassNameOfAnnotatedBeanDefinition() {

		BeanDefinition definition = new AnnotatedGenericBeanDefinition(SomeImplementation.class);

		assertThat(generator.generateBeanName(definition)).isEqualTo(SAMPLE_IMPLEMENTATION_BEAN_NAME);
	}

	private BeanDefinition getBeanDefinitionFor(Class<?> repositoryInterface) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RepositoryFactoryBeanSupport.class);
		builder.addConstructorArgValue(repositoryInterface.getName());
		return builder.getBeanDefinition();
	}

	interface PlainInterface {}

	@Named("specialName")
	interface AnnotatedInterface {}

	static class SomeImplementation {}
}
