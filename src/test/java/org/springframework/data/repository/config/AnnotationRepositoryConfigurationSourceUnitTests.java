/*
 * Copyright 2012-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
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
	BeanDefinitionRegistry registry;

	@Before
	public void setUp() {

		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		environment = new StandardEnvironment();
		resourceLoader = new DefaultResourceLoader();
		registry = mock(BeanDefinitionRegistry.class);

		source = new AnnotationRepositoryConfigurationSource(annotationMetadata, EnableRepositories.class, resourceLoader,
				environment, registry);
	}

	@Test // DATACMNS-47
	public void findsBasePackagesForClasses() {

		Iterable<String> basePackages = source.getBasePackages();
		assertThat(basePackages, hasItem(AnnotationRepositoryConfigurationSourceUnitTests.class.getPackage().getName()));
	}

	@Test // DATACMNS-47
	public void evaluatesExcludeFiltersCorrectly() {

		Collection<BeanDefinition> candidates = source.getCandidates(new DefaultResourceLoader());
		assertThat(candidates, hasSize(1));

		BeanDefinition candidate = candidates.iterator().next();
		assertThat(candidate.getBeanClassName(), is(MyRepository.class.getName()));
	}

	@Test // DATACMNS-47
	public void defaultsToPackageOfAnnotatedClass() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfiguration.class);
		Iterable<String> packages = source.getBasePackages();

		assertThat(packages, hasItem(DefaultConfiguration.class.getPackage().getName()));
		assertThat(source.shouldConsiderNestedRepositories(), is(false));
	}

	@Test // DATACMNS-47
	public void returnsConfiguredBasePackage() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);
		Iterable<String> packages = source.getBasePackages();

		assertThat(packages, hasItem("foo"));
	}

	@Test // DATACMNS-90
	public void returnsConsiderNestedRepositories() {

		AnnotationRepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithNestedRepositories.class);
		assertThat(source.shouldConsiderNestedRepositories(), is(true));
	}

	@Test // DATACMNS-456
	public void findsStringAttributeByName() {

		RepositoryConfigurationSource source = getConfigSource(DefaultConfigurationWithBasePackage.class);
		assertThat(source.getAttribute("namedQueriesLocation"), is("bar"));
	}

	@Test // DATACMNS-502
	public void returnsEmptyStringForBasePackage() throws Exception {

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				getClass().getClassLoader().loadClass("TypeInDefaultPackage"), true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				EnableRepositories.class, resourceLoader, environment, registry);

		assertThat(configurationSource.getBasePackages(), hasItem(""));
	}

	@Test // DATACMNS-526
	public void detectsExplicitFilterConfiguration() {

		assertThat(getConfigSource(ConfigurationWithExplicitFilter.class).usesExplicitFilters(), is(true));
		assertThat(getConfigSource(DefaultConfiguration.class).usesExplicitFilters(), is(false));
	}

	@Test // DATACMNS-542
	public void ignoresMissingRepositoryBaseClassNameAttribute() {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(ConfigWithSampleAnnotation.class, true);
		RepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(metadata,
				SampleAnnotation.class, resourceLoader, environment, registry);

		assertThat(configurationSource.getRepositoryBaseClassName(), is(nullValue()));
	}

	private AnnotationRepositoryConfigurationSource getConfigSource(Class<?> type) {

		AnnotationMetadata metadata = new StandardAnnotationMetadata(type, true);
		return new AnnotationRepositoryConfigurationSource(metadata, EnableRepositories.class, resourceLoader, environment,
				registry);
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
