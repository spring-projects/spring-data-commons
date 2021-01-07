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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupportUnitTests.DummyConfigurationExtension;
import org.springframework.util.ClassUtils;

/**
 * Integration tests for {@link RepositoryBeanDefinitionRegistrarSupport}.
 *
 * @author Oliver Gierke
 * @author Peter Rietzler
 * @author Mark Paluch
 */
class RepositoryBeanDefinitionRegistrarSupportIntegrationTests {

	@Configuration
	@EnableRepositories(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*\\.excluded\\..*"))
	static class SampleConfig {

	}

	@Configuration
	static class TestConfig extends SampleConfig {

	}

	AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		this.context = new AnnotationConfigApplicationContext(TestConfig.class);
	}

	@AfterEach
	void tearDown() {

		if (context != null) {
			this.context.close();
		}
	}

	@Test // DATACMNS-989
	void duplicateImplementationsMayBeExcludedViaFilters() {
		assertThat(context.getBean(MyOtherRepository.class).getImplementationId())
				.isEqualTo(MyOtherRepositoryImpl.class.getName());
	}

	@Test // DATACMNS-47
	void testBootstrappingWithInheritedConfigClasses() {

		assertThat(context.getBean(MyRepository.class)).isNotNull();
		assertThat(context.getBean(MyOtherRepository.class)).isNotNull();
	}

	@Test // DATACMNS-47
	void beanDefinitionSourceIsSetForJavaConfigScannedBeans() {

		BeanDefinition definition = context.getBeanDefinition("myRepository");
		assertThat(definition.getSource()).isNotNull();
	}

	@Test // DATACMNS-544
	void registersExtensionAsBeanDefinition() {
		assertThat(context.getBean(DummyConfigurationExtension.class)).isNotNull();
	}

	@Test // DATACMNS-102
	void composedRepositoriesShouldBeAssembledCorrectly() {
		assertThat(context.getBean(ComposedRepository.class).getOne()).isEqualTo("one");
	}

	@Test // DATACMNS-1620
	void registeredBeanDefinitionsContainHumanReadableResourceDescription() {

		BeanDefinition definition = context.getBeanDefinition("myRepository");

		assertThat(definition.getResourceDescription()) //
				.contains(MyRepository.class.getName()) //
				.contains(EnableRepositories.class.getSimpleName()) //
				.contains(ClassUtils.getShortName(SampleConfig.class));
	}
}
