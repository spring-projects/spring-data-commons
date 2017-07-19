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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import javax.inject.Named;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * Unit tests for {@link RepositoryBeanNameGenerator}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryBeanNameGeneratorUnitTests {

	static final String SAMPLE_IMPLEMENTATION_BEAN_NAME = "repositoryBeanNameGeneratorUnitTests.SomeImplementation";

	BeanNameGenerator generator;
	BeanDefinitionRegistry registry;

	@Before
	public void setUp() {

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator();
		generator.setBeanClassLoader(Thread.currentThread().getContextClassLoader());

		this.generator = generator;
		this.registry = new DefaultListableBeanFactory();
	}

	@Test
	public void usesPlainClassNameIfNoAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(MyRepository.class), registry), is("myRepository"));
	}

	@Test
	public void usesAnnotationValueIfAnnotationPresent() {
		assertThat(generator.generateBeanName(getBeanDefinitionFor(AnnotatedInterface.class), registry), is("specialName"));
	}

	@Test // DATACMNS-1115
	public void usesClassNameOfScannedBeanDefinition() throws IOException {

		MetadataReaderFactory factory = new SimpleMetadataReaderFactory();
		MetadataReader reader = factory.getMetadataReader(SomeImplementation.class.getName());

		BeanDefinition definition = new ScannedGenericBeanDefinition(reader);

		assertThat(generator.generateBeanName(definition, registry), is(SAMPLE_IMPLEMENTATION_BEAN_NAME));
	}

	@Test // DATACMNS-1115
	public void usesClassNameOfAnnotatedBeanDefinition() {

		BeanDefinition definition = new AnnotatedGenericBeanDefinition(SomeImplementation.class);

		assertThat(generator.generateBeanName(definition, registry), is(SAMPLE_IMPLEMENTATION_BEAN_NAME));
	}

	private BeanDefinition getBeanDefinitionFor(Class<?> repositoryInterface) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RepositoryFactoryBeanSupport.class);
		builder.addPropertyValue("repositoryInterface", repositoryInterface.getName());
		return builder.getBeanDefinition();
	}

	interface PlainInterface {}

	@Named("specialName")
	interface AnnotatedInterface {}

	class SomeImplementation {}
}
