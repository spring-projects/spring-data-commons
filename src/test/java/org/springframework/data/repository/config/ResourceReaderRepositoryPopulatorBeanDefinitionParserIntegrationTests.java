/*
 * Copyright 2012-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.repository.init.Jackson2ResourceReader;
import org.springframework.data.repository.init.ResourceReaderRepositoryPopulator;
import org.springframework.data.repository.init.UnmarshallingResourceReader;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for the initializer namespace elements.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class ResourceReaderRepositoryPopulatorBeanDefinitionParserIntegrationTests {

	/**
	 * @see DATACMNS-333
	 */
	@Test
	public void registersJackson2InitializerCorrectly() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(getPopulatorResource());

		BeanDefinition definition = beanFactory.getBeanDefinition("jackson2-populator");
		assertThat(definition).isNotNull();

		Object bean = beanFactory.getBean("jackson2-populator");
		assertThat(bean).isInstanceOf(ResourceReaderRepositoryPopulator.class);
		Object resourceReader = ReflectionTestUtils.getField(bean, "reader");
		assertThat(resourceReader).isInstanceOf(Jackson2ResourceReader.class);

		Object resources = ReflectionTestUtils.getField(bean, "resources");
		assertIsListOfClasspathResourcesWithPath(resources, "org/springframework/data/repository/init/data.json");
	}

	/**
	 * @see DATACMNS-58
	 */
	@Test
	public void registersXmlInitializerCorrectly() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(getPopulatorResource());

		BeanDefinition definition = beanFactory.getBeanDefinition("xml-populator");
		assertThat(definition).isNotNull();

		Object bean = beanFactory.getBean("xml-populator");
		assertThat(bean).isInstanceOf(ResourceReaderRepositoryPopulator.class);
		Object resourceReader = ReflectionTestUtils.getField(bean, "reader");
		assertThat(resourceReader).isInstanceOf(UnmarshallingResourceReader.class);
		Object unmarshaller = ReflectionTestUtils.getField(resourceReader, "unmarshaller");
		assertThat(unmarshaller).isInstanceOf(Jaxb2Marshaller.class);

		Object resources = ReflectionTestUtils.getField(bean, "resources");
		assertIsListOfClasspathResourcesWithPath(resources, "org/springframework/data/repository/init/data.xml");
	}

	private static void assertIsListOfClasspathResourcesWithPath(Object source, String path) {

		assertThat(source).isInstanceOf(List.class);
		List<?> list = (List<?>) source;
		assertThat(list).isNotEqualTo(empty());
		Object element = list.get(0);
		assertThat(element).isInstanceOf(ClassPathResource.class);
		ClassPathResource resource = (ClassPathResource) element;
		assertThat(resource.getPath()).isEqualTo(path);
	}

	private static ClassPathResource getPopulatorResource() {
		return new ClassPathResource("populators.xml",
				ResourceReaderRepositoryPopulatorBeanDefinitionParserIntegrationTests.class);
	}
}
