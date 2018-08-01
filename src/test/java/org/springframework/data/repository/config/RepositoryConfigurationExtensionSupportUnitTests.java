/*
 * Copyright 2014-2018 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Unit tests for {@link RepositoryConfigurationExtensionSupport}.
 *
 * @author Oliver Gierke
 */
public class RepositoryConfigurationExtensionSupportUnitTests {

	RepositoryConfigurationExtensionSupport extension = new SampleRepositoryConfigurationExtension();

	@Test // DATACMNS-526
	public void doesNotConsiderRepositoryForPlainTypeStrictMatch() {

		RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(PlainTypeRepository.class);
		assertThat(extension.isStrictRepositoryCandidate(metadata)).isFalse();
	}

	@Test // DATACMNS-526
	public void considersRepositoryWithAnnotatedTypeStrictMatch() {

		RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(AnnotatedTypeRepository.class);
		assertThat(extension.isStrictRepositoryCandidate(metadata)).isTrue();
	}

	@Test // DATACMNS-526
	public void considersRepositoryInterfaceExtendingStoreInterfaceStrictMatch() {

		RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(ExtendingInterface.class);
		assertThat(extension.isStrictRepositoryCandidate(metadata)).isTrue();
	}

	@Test // DATACMNS-1174
	public void rejectsReactiveRepositories() {

		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(ReactiveConfiguration.class, true);
		Environment environment = new StandardEnvironment();
		ResourceLoader resourceLoader = new DefaultResourceLoader();
		BeanDefinitionRegistry registry = mock(BeanDefinitionRegistry.class);

		RepositoryConfigurationSource source = new AnnotationRepositoryConfigurationSource(annotationMetadata,
				EnableRepositories.class, resourceLoader, environment, registry);

		assertThatThrownBy(() -> extension.getRepositoryConfigurations(source, resourceLoader))
				.isInstanceOf(InvalidDataAccessApiUsageException.class)
				.hasMessageContaining("Reactive Repositories are not supported");
	}

	static class SampleRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

		@Override
		protected String getModulePrefix() {
			return "core";
		}

		@Override
		public String getRepositoryFactoryBeanClassName() {
			return RepositoryFactorySupport.class.getName();
		}

		@Override
		protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
			return Collections.singleton(Primary.class);
		}

		@Override
		protected Collection<Class<?>> getIdentifyingTypes() {
			return Collections.singleton(StoreInterface.class);
		}
	}

	@Primary
	static class AnnotatedType {}

	static class PlainType {}

	interface AnnotatedTypeRepository extends Repository<AnnotatedType, Long> {}

	interface PlainTypeRepository extends Repository<PlainType, Long> {}

	interface ReactiveRepository extends ReactiveCrudRepository<PlainType, Long> {}

	interface StoreInterface {}

	interface ExtendingInterface extends StoreInterface, Repository<PlainType, Long> {}

	@EnableRepositories
	static class SampleConfiguration {}

	@EnableRepositories(includeFilters = { @Filter(type = FilterType.ASSIGNABLE_TYPE, value = ReactiveRepository.class) },
			basePackageClasses = ReactiveRepository.class, considerNestedRepositories = true)
	class ReactiveConfiguration {}
}
