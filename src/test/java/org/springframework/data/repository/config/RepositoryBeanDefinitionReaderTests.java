/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.aot.sample.ConfigWithCustomImplementation;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass.CustomerRepositoryWithCustomBaseRepo;
import org.springframework.data.aot.sample.ConfigWithFragments;
import org.springframework.data.aot.sample.ConfigWithSimpleCrudRepository;
import org.springframework.data.aot.sample.ReactiveConfig;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;

/**
 * Unit tests for {@link RepositoryBeanDefinitionReader}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class RepositoryBeanDefinitionReaderTests {

	@Test // GH-3279
	void readsSimpleConfigFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithSimpleCrudRepository.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		when(repoConfig.getRepositoryInterface()).thenReturn(ConfigWithSimpleCrudRepository.MyRepo.class.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getRepositoryInterface()).isEqualTo(ConfigWithSimpleCrudRepository.MyRepo.class);
		assertThat(repositoryInformation.getDomainType()).isEqualTo(ConfigWithSimpleCrudRepository.Person.class);
		assertThat(repositoryInformation.getFragments()).isEmpty();
	}

	@Test // GH-3279
	void readsCustomRepoBaseClassFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithCustomRepositoryBaseClass.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Class<?> repositoryInterfaceType = CustomerRepositoryWithCustomBaseRepo.class;
		when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getRepositoryBaseClass())
				.isEqualTo(ConfigWithCustomRepositoryBaseClass.RepoBaseClass.class);
	}

	@Test // GH-3279
	void readsFragmentsContributorFromBeanDefinition() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithCustomRepositoryBaseClass.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Class<?> repositoryInterfaceType = CustomerRepositoryWithCustomBaseRepo.class;
		when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getFragments())
				.contains(RepositoryFragment.structural(SampleRepositoryFragmentsContributor.class));
	}

	@Test // GH-3279
	void readsFragmentsContributorFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ReactiveConfig.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Class<?> repositoryInterfaceType = ReactiveConfig.CustomerRepositoryReactive.class;
		when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getFragments()).isEmpty();
	}

	@Test // GH-3279, GH-3282, GH-3423
	void readsCustomImplementationFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithCustomImplementation.class);
		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);

		Class<?> repositoryInterfaceType = ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class;
		when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getRepositoryBaseClass()).isEqualTo(PagingAndSortingRepository.class);
		assertThat(repositoryInformation.getFragments()).satisfiesExactly(fragment -> {
			assertThat(fragment.getImplementationClass())
					.contains(ConfigWithCustomImplementation.RepositoryWithCustomImplementationImpl.class);
		});
	}

	@Test // GH-3279, GH-3282
	void readsFragmentsFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithFragments.class);
		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);

		Class<?> repositoryInterfaceType = ConfigWithFragments.RepositoryWithFragments.class;
		when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(repoFactoryBean);
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();

		assertThat(repositoryInformation.getFragments()).hasSize(2);

		for (RepositoryFragment<?> fragment : repositoryInformation.getFragments()) {

			assertThat(fragment.getSignatureContributor()).isIn(ConfigWithFragments.CustomImplInterface1.class,
					ConfigWithFragments.CustomImplInterface2.class);

			assertThat(fragment.getImplementationClass().get()).isIn(ConfigWithFragments.CustomImplInterface1Impl.class,
					ConfigWithFragments.CustomImplInterface2Impl.class);
		}
	}

	static RegisteredBean repositoryFactory(Class<?> configClass) {

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configClass);
		applicationContext.refreshForAotProcessing(new RuntimeHints());

		String[] beanNamesForType = applicationContext.getBeanNamesForType(RepositoryFactoryBeanSupport.class);
		if (beanNamesForType.length != 1) {
			throw new IllegalStateException("Unable to find repository FactoryBean");
		}

		return RegisteredBean.of(applicationContext.getBeanFactory(), beanNamesForType[0]);
	}

}
