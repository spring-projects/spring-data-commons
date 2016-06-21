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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;

/**
 * Integration test for {@link RepositoryBeanDefinitionRegistrarSupport}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryBeanDefinitionRegistrarSupportUnitTests {

	@Mock BeanDefinitionRegistry registry;

	StandardEnvironment environment;
	DummyRegistrar registrar;

	@Before
	public void setUp() {

		environment = new StandardEnvironment();

		registrar = new DummyRegistrar();
		registrar.setEnvironment(environment);
	}

	@Test
	public void registersBeanDefinitionForFoundBean() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);

		registrar.registerBeanDefinitions(metadata, registry);

		assertBeanDefinitionRegisteredFor("myRepository");
		assertNoBeanDefinitionRegisteredFor("profileRepository");
	}

	/**
	 * @see DATACMNS-360
	 */
	@Test
	public void registeredProfileRepositoriesIfProfileActivated() {

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		environment.setActiveProfiles("profile");

		DummyRegistrar registrar = new DummyRegistrar();
		registrar.setEnvironment(environment);

		registrar.registerBeanDefinitions(metadata, registry);

		assertBeanDefinitionRegisteredFor("myRepository", "profileRepository");
	}

	private void assertBeanDefinitionRegisteredFor(String... names) {

		for (String name : names) {
			verify(registry, times(1)).registerBeanDefinition(eq(name), any(BeanDefinition.class));
		}
	}

	private void assertNoBeanDefinitionRegisteredFor(String... names) {

		for (String name : names) {
			verify(registry, times(0)).registerBeanDefinition(eq(name), any(BeanDefinition.class));
		}
	}

	static class DummyRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

		DummyRegistrar() {
			setResourceLoader(new DefaultResourceLoader());
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport#getAnnotation()
		 */
		@Override
		protected Class<? extends Annotation> getAnnotation() {
			return EnableRepositories.class;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport#getExtension()
		 */
		@Override
		protected RepositoryConfigurationExtension getExtension() {
			return new DummyConfigurationExtension();
		}
	}

	static class DummyConfigurationExtension extends RepositoryConfigurationExtensionSupport {

		public String getRepositoryFactoryClassName() {
			return DummyRepositoryFactoryBean.class.getName();
		}

		@Override
		protected String getModulePrefix() {
			return "commons";
		}
	}
}
