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
import static org.mockito.Mockito.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link AnnotationRepositoryConfigurationSource}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
class AnnotationRepositoryConfigurationSourceUnitTests {

	RepositoryConfigurationSource source;
	Environment environment;
	ResourceLoader resourceLoader;
	BeanDefinitionRegistry registry;

	@BeforeEach
	void setUp() {

		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		environment = new StandardEnvironment();
		resourceLoader = new DefaultResourceLoader();
		registry = mock(BeanDefinitionRegistry.class);

		source = new AnnotationRepositoryConfigurationSource(annotationMetadata, EnableRepositories.class, resourceLoader,
				environment, registry);
	}

	@Test // DATACMNS-47
	void findsBasePackagesForClasses() {

		assertThat(source.getBasePackages())//
				.contains(AnnotationRepositoryConfigurationSourceUnitTests.class.getPackage().getName());
	}

	@Test // DATACMNS-47, DATACMNS-102
	void evaluatesExcludeFiltersCorrectly() {

		Streamable<BeanDefinition> candidates = source.getCandidates(new DefaultResourceLoader());

		assertThat(candidates).extracting("beanClassName")
				.contains(MyRepository.class.getName(), ComposedRepository.class.getName())
				.doesNotContain(MyOtherRepository.class.getName(), ExcludedRepository.class.getName());
	}

	@Test // DATACMNS-47
	void defaultsToPackageOfAnnotatedClass() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);
		Iterable<String> packages = source.getBasePackages();

		assertThat(packages).contains(DefaultConfiguration.class.getPackage().getName());
		assertThat(source.shouldConsiderNestedRepositories()).isFalse();
	}

	@Test // DATACMNS-47
	void returnsConfiguredBasePackage() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);

		assertThat(source.getBasePackages()).contains("foo");
	}

	@Test // DATACMNS-90
	void returnsConsiderNestedRepositories() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithNestedRepositories.class);
		assertThat(source.shouldConsiderNestedRepositories()).isTrue();
	}

	@Test // DATACMNS-456
	void findsStringAttributeByName() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);
		assertThat(source.getAttribute("namedQueriesLocation")).hasValue("bar");
	}

	@Test // DATACMNS-502
	void returnsEmptyStringForBasePackage() throws Exception {

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				getClass().getClassLoader().loadClass("TypeInDefaultPackage"), true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, resourceLoader, environment, registry);

		assertThat(configurationSource.getBasePackages()).contains("");
	}

	@Test // DATACMNS-526
	void detectsExplicitFilterConfiguration() {

		assertThat(getConfigSource(ConfigurationWithExplicitFilter.class).usesExplicitFilters()).isTrue();
		assertThat(getConfigSource(DefaultConfiguration.class).usesExplicitFilters()).isFalse();
	}

	@Test // DATACMNS-542
	void ignoresMissingRepositoryBaseClassNameAttribute() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(ConfigWithSampleAnnotation.class, true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				SampleAnnotation.class, resourceLoader, environment, registry);

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

	private AnnotationRepositoryConfigurationSource getConfigSource(Class<?> type) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(type, true);
		return new AnnotationRepositoryConfigurationSource(metadata, EnableRepositories.class, resourceLoader, environment,
				registry);
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

	@Retention(RetentionPolicy.RUNTIME)
	@interface SampleAnnotation {

		Filter[] includeFilters() default {};

		Filter[] excludeFilters() default {};
	}

	@SampleAnnotation
	static class ConfigWithSampleAnnotation {}
}
