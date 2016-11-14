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

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;

/**
 * Unit tests for {@link AnnotationRepositoryConfigurationSource}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AnnotationRepositoryConfigurationSourceUnitTests {

	RepositoryConfigurationSource source;
	Environment environment;
	ResourceLoader resourceLoader;

	@Before
	public void setUp() {

		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		environment = new StandardEnvironment();
		resourceLoader = new DefaultResourceLoader();
		source = new AnnotationRepositoryConfigurationSource(annotationMetadata, EnableRepositories.class, resourceLoader,
				environment);
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void findsBasePackagesForClasses() {

		assertThat(source.getBasePackages())//
				.contains(AnnotationRepositoryConfigurationSourceUnitTests.class.getPackage().getName());
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void evaluatesExcludeFiltersCorrectly() {

		Collection<BeanDefinition> candidates = source.getCandidates(new DefaultResourceLoader());
		assertThat(candidates).hasSize(1);

		BeanDefinition candidate = candidates.iterator().next();
		assertThat(candidate.getBeanClassName()).isEqualTo(MyRepository.class.getName());
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void defaultsToPackageOfAnnotatedClass() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);
		Iterable<String> packages = source.getBasePackages();

		assertThat(packages).contains(DefaultConfiguration.class.getPackage().getName());
		assertThat(source.shouldConsiderNestedRepositories()).isFalse();
	}

	/**
	 * @see DATACMNS-47
	 */
	@Test
	public void returnsConfiguredBasePackage() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);

		assertThat(source.getBasePackages()).contains("foo");
	}

	/**
	 * @see DATACMNS-90
	 */
	@Test
	public void returnsConsiderNestedRepositories() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithNestedRepositories.class);
		assertThat(source.shouldConsiderNestedRepositories()).isTrue();
	}

	/**
	 * @see DATACMNS-456
	 */
	@Test
	public void findsStringAttributeByName() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);
		assertThat(source.getAttribute("namedQueriesLocation")).isEqualTo("bar");
	}

	/**
	 * @see DATACMNS-502
	 */
	@Test
	public void returnsEmptyStringForBasePackage() throws Exception {

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				getClass().getClassLoader().loadClass("TypeInDefaultPackage"), true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, resourceLoader, environment);

		assertThat(configurationSource.getBasePackages()).contains("");
	}

	/**
	 * @see DATACMNS-526
	 */
	@Test
	public void detectsExplicitFilterConfiguration() {

		assertThat(getConfigSource(ConfigurationWithExplicitFilter.class).usesExplicitFilters()).isTrue();
		assertThat(getConfigSource(DefaultConfiguration.class).usesExplicitFilters()).isFalse();
	}

	/**
	 * @see DATACMNS-542
	 */
	@Test
	public void ignoresMissingRepositoryBaseClassNameAttribute() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(ConfigWithSampleAnnotation.class, true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				SampleAnnotation.class, resourceLoader, environment);

		assertThat(configurationSource.getRepositoryBaseClassName()).isNull();
	}

	private AnnotationRepositoryConfigurationSource getConfigSource(Class<?> type) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(type, true);
		return new AnnotationRepositoryConfigurationSource(metadata, EnableRepositories.class, resourceLoader, environment);
	}

	public static class Person {}

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
