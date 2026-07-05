/*
 * Copyright 2012-present the original author or authors.
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
import static org.mockito.Mockito.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.config.basepackage.repo.PersonRepository;
import org.springframework.data.repository.core.support.DummyReactiveRepositoryFactory;
import org.springframework.data.repository.core.support.DummyRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Unit tests for {@link AnnotationRepositoryConfigurationSource}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class AnnotationRepositoryConfigurationSourceUnitTests {

	RepositoryConfigurationSource source;
	Environment environment;
	ResourceLoader resourceLoader;
	BeanDefinitionRegistry registry;

	@BeforeEach
	void setUp() {

		AnnotationMetadata annotationMetadata = AnnotationMetadata.introspect(SampleConfiguration.class);
		environment = new StandardEnvironment();
		resourceLoader = new DefaultResourceLoader();
		registry = mock(BeanDefinitionRegistry.class);

		source = new AnnotationRepositoryConfigurationSource(annotationMetadata, EnableRepositories.class, resourceLoader,
				environment, registry, null);
	}

	@Test // DATACMNS-47
	void findsBasePackagesForClasses() {

		assertThat(source.getBasePackages())//
				.contains(AnnotationRepositoryConfigurationSourceUnitTests.class.getPackage().getName());
	}

	@Test // DATACMNS-47, DATACMNS-102
	void evaluatesExcludeFiltersCorrectly() {

		var candidates = source.getCandidates(new DefaultResourceLoader());

		assertThat(candidates).extracting("beanClassName")
				.contains(MyRepository.class.getName(), ComposedRepository.class.getName())
				.doesNotContain(MyOtherRepository.class.getName(), ExcludedRepository.class.getName());
	}

	@Test // DATACMNS-47
	void defaultsToPackageOfAnnotatedClass() {

		var source = getConfigSource(DefaultConfiguration.class);
		Iterable<String> packages = source.getBasePackages();

		assertThat(packages).contains(DefaultConfiguration.class.getPackage().getName());
		assertThat(source.shouldConsiderNestedRepositories()).isFalse();
	}

	@Test // DATACMNS-47
	void returnsConfiguredBasePackage() {

		var source = getConfigSource(DefaultConfigurationWithBasePackage.class);

		assertThat(source.getBasePackages()).contains("foo");
	}

	@Test // DATACMNS-90
	void returnsConsiderNestedRepositories() {

		var source = getConfigSource(DefaultConfigurationWithNestedRepositories.class);
		assertThat(source.shouldConsiderNestedRepositories()).isTrue();
	}

	@Test // DATACMNS-456
	void findsStringAttributeByName() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);
		assertThat(source.getAttribute("namedQueriesLocation")).hasValue("bar");
	}

	@Test // DATACMNS-502
	void returnsEmptyStringForBasePackage() throws Exception {

		var metadata = AnnotationMetadata.introspect(getClass().getClassLoader().loadClass("TypeInDefaultPackage"));
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, resourceLoader, environment, registry, null);

		assertThat(configurationSource.getBasePackages()).contains("");
	}

	@Test // DATACMNS-526
	void detectsExplicitFilterConfiguration() {

		assertThat(getConfigSource(ConfigurationWithExplicitFilter.class).usesExplicitFilters()).isTrue();
		assertThat(getConfigSource(DefaultConfiguration.class).usesExplicitFilters()).isFalse();
	}

	@Test // DATACMNS-542
	void ignoresMissingRepositoryBaseClassNameAttribute() {

		AnnotationMetadata metadata = AnnotationMetadata.introspect(ConfigWithSampleAnnotation.class);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				SampleAnnotation.class, resourceLoader, environment, registry, null);

		assertThat(configurationSource.getRepositoryBaseClassName()).isNotPresent();
	}

	@Test // DATACMNS-1498
	void allowsLookupOfNonStringAttribute() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);

		assertThat(source.getAttribute("repositoryBaseClass", Class.class)).hasValue(PagingAndSortingRepository.class);
		assertThat(source.getRequiredAttribute("repositoryBaseClass", Class.class))
				.isEqualTo(PagingAndSortingRepository.class);
	}

	@Test // DATACMNS-1498
	void rejectsInvalidAttributeName() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);

		assertThatIllegalArgumentException().isThrownBy(() -> source.getAttribute("fooBar"));
	}

	@Test // DATACMNS-1498
	void lookupOfEmptyStringExposesAbsentValue() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);

		assertThat(source.getAttribute("namedQueriesLocation", String.class)).isEmpty();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> source.getRequiredAttribute("namedQueriesLocation", String.class));
	}

	@Test // GH-3082
	void considerBeanNameGenerator() {

		RootBeanDefinition bd = new RootBeanDefinition(DummyRepositoryFactory.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue(PersonRepository.class);

		assertThat(getConfigSource(ConfigurationWithBeanNameGenerator.class).generateBeanName(bd))
				.isEqualTo("org.springframework.data.repository.config.basepackage.repo.PersonRepository");
		assertThat(getConfigSource(DefaultConfiguration.class).generateBeanName(bd)).isEqualTo("personRepository");
	}

	@Test // GH-3279
	void considersDefaultFragmentsContributor() {

		RootBeanDefinition bd = new RootBeanDefinition(DummyRepositoryFactory.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue(PersonRepository.class);

		AnnotationMetadata metadata = AnnotationMetadata.introspect(ConfigurationWithFragmentsContributor.class);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositoriesWithContributor.class, resourceLoader, environment, registry, null);

		assertThat(configurationSource.getRepositoryFragmentsContributorClassName())
				.contains(SampleRepositoryFragmentsContributor.class.getName());
	}

	@Test // GH-3279
	void skipsInterfaceFragmentsContributor() {

		RootBeanDefinition bd = new RootBeanDefinition(DummyRepositoryFactory.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue(PersonRepository.class);

		AnnotationMetadata metadata = AnnotationMetadata.introspect(ConfigurationWithFragmentsContributorInterface.class);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositoriesWithContributor.class, resourceLoader, environment, registry, null);

		assertThat(configurationSource.getRepositoryFragmentsContributorClassName()).isEmpty();
	}

	@Test // GH-3279
	void omitsUnspecifiedFragmentsContributor() {

		RootBeanDefinition bd = new RootBeanDefinition(DummyRepositoryFactory.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue(PersonRepository.class);

		AnnotationMetadata metadata = AnnotationMetadata.introspect(ReactiveConfigurationWithBeanNameGenerator.class);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableReactiveRepositories.class, resourceLoader, environment, registry, null);

		assertThat(configurationSource.getRepositoryFragmentsContributorClassName()).isEmpty();
	}

	@Test // GH-3082
	void considerBeanNameGeneratorForReactiveRepos() {

		RootBeanDefinition bd = new RootBeanDefinition(DummyReactiveRepositoryFactory.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue(ReactivePersonRepository.class);

		assertThat(getConfigSource(ConfigurationWithBeanNameGenerator.class).generateBeanName(bd))
				.isEqualTo(ReactivePersonRepository.class.getName());
		assertThat(getConfigSource(DefaultConfiguration.class).generateBeanName(bd))
				.isEqualTo("annotationRepositoryConfigurationSourceUnitTests.ReactivePersonRepository");
	}

	private AnnotationRepositoryConfigurationSource getConfigSource(Class<?> type) {

		AnnotationMetadata metadata = AnnotationMetadata.introspect(type);
		return new AnnotationRepositoryConfigurationSource(metadata, EnableRepositories.class, resourceLoader, environment,
				registry, null);
	}

	static class Person {}

	@EnableRepositories
	static class DefaultConfiguration {}

	@EnableRepositories(basePackages = "foo", namedQueriesLocation = "bar")
	static class DefaultConfigurationWithBasePackage {}

	@EnableRepositories(considerNestedRepositories = true)
	static class DefaultConfigurationWithNestedRepositories {}

	@EnableRepositories(excludeFilters = { @Filter(Primary.class) })
	static class ConfigurationWithExplicitFilter {}

	@EnableRepositories(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
	static class ConfigurationWithBeanNameGenerator {}

	@EnableRepositoriesWithContributor()
	static class ConfigurationWithFragmentsContributor {}

	@EnableRepositoriesWithContributor(fragmentsContributor = RepositoryFragmentsContributor.class)
	static class ConfigurationWithFragmentsContributorInterface {}

	@EnableReactiveRepositories(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
	static class ReactiveConfigurationWithBeanNameGenerator {}

	@Retention(RetentionPolicy.RUNTIME)
	@interface SampleAnnotation {

		Filter[] includeFilters() default {};

		Filter[] excludeFilters() default {};

	}

	@SampleAnnotation
	static class ConfigWithSampleAnnotation {}

	interface ReactivePersonRepository extends ReactiveCrudRepository<Person, String> {}

}
