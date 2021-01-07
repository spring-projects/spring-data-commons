/*
 * Copyright 2016-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate.LazyRepositoryInjectionPointResolver;
import org.springframework.data.repository.sample.AddressRepository;
import org.springframework.data.repository.sample.AddressRepositoryClient;
import org.springframework.data.repository.sample.ProductRepository;

/**
 * Unit tests for {@link RepositoryConfigurationDelegate}.
 *
 * @author Oliver Gierke
 * @soundtrack Richard Spaven - Tribute (Whole Other*)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryConfigurationDelegateUnitTests {

	@Mock RepositoryConfigurationExtension extension;

	@Test // DATACMNS-892
	void registersRepositoryBeanNameAsAttribute() {

		StandardEnvironment environment = new StandardEnvironment();
		GenericApplicationContext context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				new StandardAnnotationMetadata(TestConfig.class, true), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory());

		RepositoryConfigurationDelegate delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		for (BeanComponentDefinition definition : delegate.registerRepositoriesIn(context, extension)) {

			BeanDefinition beanDefinition = definition.getBeanDefinition();

			assertThat(beanDefinition.getAttribute(RepositoryConfigurationDelegate.FACTORY_BEAN_OBJECT_TYPE).toString())
					.endsWith("Repository");
		}
	}

	@Test // DATACMNS-1368
	void registersLazyAutowireCandidateResolver() {
		assertLazyRepositoryBeanSetup(LazyConfig.class);
	}

	@Test // DATACMNS-1368
	void registersDeferredRepositoryInitializationListener() {

		ListableBeanFactory beanFactory = assertLazyRepositoryBeanSetup(DeferredConfig.class);

		assertThat(beanFactory.getBeanNamesForType(DeferredRepositoryInitializationListener.class)).isNotEmpty();

	}

	private static ListableBeanFactory assertLazyRepositoryBeanSetup(Class<?> configClass) {

		StandardEnvironment environment = new StandardEnvironment();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);

		assertThat(context.getDefaultListableBeanFactory().getAutowireCandidateResolver())
				.isInstanceOf(LazyRepositoryInjectionPointResolver.class);

		AddressRepositoryClient client = context.getBean(AddressRepositoryClient.class);
		AddressRepository repository = client.getRepository();

		assertThat(Advised.class.isInstance(repository)).isTrue();

		TargetSource targetSource = Advised.class.cast(repository).getTargetSource();
		assertThat(targetSource).isNotNull();

		return context.getDefaultListableBeanFactory();
	}

	@EnableRepositories(basePackageClasses = ProductRepository.class)
	static class TestConfig {}

	@ComponentScan(basePackageClasses = AddressRepository.class)
	@EnableRepositories(basePackageClasses = AddressRepository.class,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AddressRepository.class),
			bootstrapMode = BootstrapMode.LAZY)
	static class LazyConfig {}

	@ComponentScan(basePackageClasses = AddressRepository.class)
	@EnableRepositories(basePackageClasses = AddressRepository.class,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AddressRepository.class),
			bootstrapMode = BootstrapMode.DEFERRED)
	static class DeferredConfig {}
}
