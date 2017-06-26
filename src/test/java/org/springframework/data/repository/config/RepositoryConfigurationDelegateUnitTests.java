/*
 * Copyright 2016 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.sample.ProductRepository;

/**
 * Unit tests for {@link RepositoryConfigurationDelegate}.
 * 
 * @author Oliver Gierke
 * @soundtrack Richard Spaven - Tribute (Whole Other*)
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryConfigurationDelegateUnitTests {

	@Mock RepositoryConfigurationExtension extension;

	/**
	 * @see DATACMNS-892
	 */
	@Test
	public void registersRepositoryBeanNameAsAttribute() {

		StandardEnvironment environment = new StandardEnvironment();
		GenericApplicationContext context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				new StandardAnnotationMetadata(TestConfig.class, true), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory());

		RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		for (BeanComponentDefinition definition : delegate.registerRepositoriesIn(context, extension)) {

			BeanDefinition beanDefinition = definition.getBeanDefinition();

			assertThat(beanDefinition.getAttribute(RepositoryConfigurationDelegate.FACTORY_BEAN_OBJECT_TYPE).toString(),
					endsWith("Repository"));
		}
	}

	@EnableRepositories(basePackageClasses = ProductRepository.class)
	static class TestConfig {}
}
