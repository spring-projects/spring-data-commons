/*
 * Copyright 2014 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Unit tests for {@link RepositoryConfigurationExtensionSupport}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryConfigurationExtensionSupportUnitTests {

	RepositoryConfigurationExtensionSupport extension = new SampleRepositoryConfigurationExtension();

	/**
	 * @see DATACMNS-526
	 */
	@Test
	public void doesNotConsiderRepositoryForPlainTypeStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(PlainTypeRepository.class), is(false));
	}

	/**
	 * @see DATACMNS-526
	 */
	@Test
	public void considersRepositoryWithAnnotatedTypeStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(AnnotatedTypeRepository.class), is(true));
	}

	/**
	 * @see DATACMNS-526
	 */
	@Test
	public void considersRepositoryInterfaceExtendingStoreInterfaceStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(ExtendingInterface.class), is(true));
	}

	/**
	 * @see DATACMNS-609
	 */
	@Test
	public void registersRepositoryInterfaceAwareBeanPostProcessorOnlyOnceForMultipleConfigurations() {

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AnnotationMetadata annotationMetadata = new StandardAnnotationMetadata(SampleConfiguration.class, true);
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		StandardEnvironment environment = new StandardEnvironment();

		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				annotationMetadata, EnableRepositories.class, resourceLoader, environment, beanFactory);

		extension.registerBeansForRoot(beanFactory, configurationSource);
		extension.registerBeansForRoot(beanFactory, configurationSource);

		assertThat(beanFactory.getBeanDefinitionCount(), is(1));
	}

	static class SampleRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

		@Override
		protected String getModulePrefix() {
			return "core";
		}

		@Override
		public String getRepositoryFactoryClassName() {
			return RepositoryFactorySupport.class.getName();
		}

		@Override
		protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
			return Collections.<Class<? extends Annotation>> singleton(Primary.class);
		}

		@Override
		protected Collection<Class<?>> getIdentifyingTypes() {
			return Collections.<Class<?>> singleton(StoreInterface.class);
		}
	}

	@Primary
	static class AnnotatedType {}

	static class PlainType {}

	interface AnnotatedTypeRepository extends Repository<AnnotatedType, Long> {}

	interface PlainTypeRepository extends Repository<PlainType, Long> {}

	interface StoreInterface {}

	interface ExtendingInterface extends StoreInterface, Repository<PlainType, Long> {}

	@EnableRepositories
	static class SampleConfiguration {}
}
