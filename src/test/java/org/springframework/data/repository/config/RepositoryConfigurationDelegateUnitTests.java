/*
 * Copyright 2016-2023 the original author or authors.
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.config.RepositoryConfigurationDelegate.LazyRepositoryInjectionPointResolver;
import org.springframework.data.repository.config.annotated.MyAnnotatedRepository;
import org.springframework.data.repository.config.annotated.MyAnnotatedRepositoryImpl;
import org.springframework.data.repository.config.annotated.MyFragmentImpl;
import org.springframework.data.repository.config.excluded.MyOtherRepositoryImpl;
import org.springframework.data.repository.config.stereotype.MyStereotypeRepository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.sample.AddressRepository;
import org.springframework.data.repository.sample.AddressRepositoryClient;
import org.springframework.data.repository.sample.ProductRepository;

/**
 * Unit tests for {@link RepositoryConfigurationDelegate}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Richard Spaven - Tribute (Whole Other*)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryConfigurationDelegateUnitTests {

	RepositoryConfigurationExtension extension = new DummyConfigurationExtension();

	@Test // DATACMNS-892
	void registersRepositoryBeanNameAsAttribute() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				new StandardAnnotationMetadata(TestConfig.class, true), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		for (var definition : delegate.registerRepositoriesIn(context, extension)) {

			var beanDefinition = definition.getBeanDefinition();

			assertThat(beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE).toString()).endsWith("Repository");
		}
	}

	@Test // DATACMNS-1368
	void registersLazyAutowireCandidateResolver() {
		assertLazyRepositoryBeanSetup(LazyConfig.class);
	}

	@Test // DATACMNS-1368
	void registersDeferredRepositoryInitializationListener() {

		var beanFactory = assertLazyRepositoryBeanSetup(DeferredConfig.class);

		assertThat(beanFactory.getBeanNamesForType(DeferredRepositoryInitializationListener.class)).isNotEmpty();

	}

	@Test // DATACMNS-1832
	void writesRepositoryScanningMetrics() {

		var startup = Mockito.spy(ApplicationStartup.DEFAULT);

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();
		context.setApplicationStartup(startup);

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				new StandardAnnotationMetadata(TestConfig.class, true), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		delegate.registerRepositoriesIn(context, extension);

		Mockito.verify(startup).start("spring.data.repository.scanning");
	}

	@Test // GH-2487
	void considersDefaultBeanNames() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(DefaultBeanNamesConfig.class), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		delegate.registerRepositoriesIn(context, extension);

		assertThat(context.getBeanFactory().getBeanDefinition("myOtherRepository")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("myOtherRepositoryImpl")).isNotNull();
	}

	@Test // GH-2487
	void considersAnnotatedBeanNamesFromRepository() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(AnnotatedDerivedBeanNamesConfig.class), EnableRepositories.class, context,
				environment, context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		delegate.registerRepositoriesIn(context, extension);

		assertThat(context.getBeanFactory().getBeanDefinition("fooRepository")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("fooRepositoryImpl")).isNotNull();
	}

	@Test // GH-2487
	void considersAnnotatedBeanNamesFromAtComponent() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(AnnotatedBeanNamesConfig.class), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		delegate.registerRepositoriesIn(context, extension);

		assertThat(context.getBeanFactory().getBeanDefinition("myAnnotatedRepository")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("anotherBeanName")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("fragment")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("fragmentFragment")).isNotNull();
	}

	@Test // GH-2487
	void skipsRegistrationOnAlreadyRegisteredBeansUsingAtComponentNames() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);
		context.registerBean("fragment", MyFragmentImpl.class);
		context.registerBean("anotherBeanName", MyAnnotatedRepositoryImpl.class);

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(AnnotatedBeanNamesConfig.class), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		var delegate = new RepositoryConfigurationDelegate(configSource, context, environment);

		delegate.registerRepositoriesIn(context, extension);

		assertThat(context.getBeanFactory().getBeanDefinition("myAnnotatedRepository")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("anotherBeanName")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("fragment")).isNotNull();
		assertThat(context.getBeanFactory().getBeanDefinition("fragmentFragment")).isNotNull();
	}

	@Test // GH-2760
	void registersAotPostProcessorForDifferentConfigurations() {

		var environment = new StandardEnvironment();
		var context = new GenericApplicationContext();
		context.setAllowBeanDefinitionOverriding(false);

		RepositoryConfigurationSource configSource = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(AnnotatedBeanNamesConfig.class), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		new RepositoryConfigurationDelegate(configSource, context, environment).registerRepositoriesIn(context, extension);

		RepositoryConfigurationSource configSource2 = new AnnotationRepositoryConfigurationSource(
				AnnotationMetadata.introspect(TestConfig.class), EnableRepositories.class, context, environment,
				context.getDefaultListableBeanFactory(), new AnnotationBeanNameGenerator());

		new RepositoryConfigurationDelegate(configSource2, context, environment).registerRepositoriesIn(context, extension);

		context.refreshForAotProcessing(new RuntimeHints());
		assertThat(context.getBeanNamesForType(RepositoryRegistrationAotProcessor.class)).hasSize(2);
	}

	private static ListableBeanFactory assertLazyRepositoryBeanSetup(Class<?> configClass) {

		var context = new AnnotationConfigApplicationContext(configClass);

		assertThat(context.getDefaultListableBeanFactory().getAutowireCandidateResolver())
				.isInstanceOf(LazyRepositoryInjectionPointResolver.class);

		var client = context.getBean(AddressRepositoryClient.class);
		var repository = client.getRepository();

		assertThat(Advised.class.isInstance(repository)).isTrue();

		var targetSource = Advised.class.cast(repository).getTargetSource();
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

	@EnableRepositories(basePackageClasses = MyOtherRepository.class,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MyOtherRepository.class),
			excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MyOtherRepositoryImpl.class))
	static class DefaultBeanNamesConfig {}

	@EnableRepositories(basePackageClasses = MyStereotypeRepository.class,
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = MyStereotypeRepository.class))
	static class AnnotatedDerivedBeanNamesConfig {}

	@EnableRepositories(basePackageClasses = MyAnnotatedRepository.class)
	static class AnnotatedBeanNamesConfig {}

	static class DummyConfigurationExtension extends RepositoryConfigurationExtensionSupport {

		public String getRepositoryFactoryBeanClassName() {
			return DummyRepositoryFactoryBean.class.getName();
		}

		@Override
		protected String getModulePrefix() {
			return "commons";
		}
	}
}
