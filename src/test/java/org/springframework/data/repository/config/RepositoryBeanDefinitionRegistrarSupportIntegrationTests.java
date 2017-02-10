/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupportUnitTests.DummyConfigurationExtension;

/**
 * Integration tests for {@link RepositoryBeanDefinitionRegistrarSupport}.
 * 
 * @author Oliver Gierke
 * @author Peter Rietzler
 */
public class RepositoryBeanDefinitionRegistrarSupportIntegrationTests {

	@Configuration
	@EnableRepositories(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.excluded\\..*"))
	static class SampleConfig {

	}

	@Configuration
	static class TestConfig extends SampleConfig {

	}

	AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext(TestConfig.class);
	}

	@After
	public void tearDown() {

		if (context != null) {
			this.context.close();
		}
	}

	/**
	 * @see DATACMNS-989
	 */
	@Test
	public void duplicateImplementationsMayBeExcludedViaFilters() {
		assertThat(context.getBean(MyOtherRepository.class).getImplementationId(), is(MyOtherRepositoryImpl.class.getName()));
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void testBootstrappingWithInheritedConfigClasses() {

		assertThat(context.getBean(MyRepository.class), is(notNullValue()));
		assertThat(context.getBean(MyOtherRepository.class), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void beanDefinitionSourceIsSetForJavaConfigScannedBeans() {

		BeanDefinition definition = context.getBeanDefinition("myRepository");
		assertThat(definition.getSource(), is(notNullValue()));
	}

	/**
	 * @see DATACMNS-544
	 */
	@Test
	public void registersExtensionAsBeanDefinition() {
		assertThat(context.getBean(DummyConfigurationExtension.class), is(notNullValue()));
	}
}
