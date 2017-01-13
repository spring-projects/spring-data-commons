/*
 * Copyright 2014-2017 the original author or authors.
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
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Unit tests for {@link RepositoryConfigurationExtensionSupport}.
 * 
 * @author Oliver Gierke
 */
public class RepositoryConfigurationExtensionSupportUnitTests {

	RepositoryConfigurationExtensionSupport extension = new SampleRepositoryConfigurationExtension();

	@Test // DATACMNS-526
	public void doesNotConsiderRepositoryForPlainTypeStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(PlainTypeRepository.class), is(false));
	}

	@Test // DATACMNS-526
	public void considersRepositoryWithAnnotatedTypeStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(AnnotatedTypeRepository.class), is(true));
	}

	@Test // DATACMNS-526
	public void considersRepositoryInterfaceExtendingStoreInterfaceStrictMatch() {
		assertThat(extension.isStrictRepositoryCandidate(ExtendingInterface.class), is(true));
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
