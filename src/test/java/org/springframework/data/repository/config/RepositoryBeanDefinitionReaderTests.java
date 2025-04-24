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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.aot.sample.ConfigWithCustomImplementation;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass.CustomerRepositoryWithCustomBaseRepo;
import org.springframework.data.aot.sample.ConfigWithSimpleCrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;

/**
 * @author Christoph Strobl
 */
class RepositoryBeanDefinitionReaderTests {

	@Test // GH-3279
	void readsSimpleConfigFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithSimpleCrudRepository.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Mockito.when(repoConfig.getRepositoryInterface()).thenReturn(ConfigWithSimpleCrudRepository.MyRepo.class.getName());

		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.repositoryInformation(repoConfig,
				repoFactoryBean.getMergedBeanDefinition(), repoFactoryBean.getBeanFactory());

		assertThat(repositoryInformation.getRepositoryInterface()).isEqualTo(ConfigWithSimpleCrudRepository.MyRepo.class);
		assertThat(repositoryInformation.getDomainType()).isEqualTo(ConfigWithSimpleCrudRepository.Person.class);
		assertThat(repositoryInformation.getFragments()).isEmpty();
	}

	@Test // GH-3279
	void readsCustomRepoBaseClassFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithCustomRepositoryBaseClass.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Class<?> repositoryInterfaceType = CustomerRepositoryWithCustomBaseRepo.class;
		Mockito.when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.repositoryInformation(repoConfig,
				repoFactoryBean.getMergedBeanDefinition(), repoFactoryBean.getBeanFactory());

		assertThat(repositoryInformation.getRepositoryBaseClass())
				.isEqualTo(ConfigWithCustomRepositoryBaseClass.RepoBaseClass.class);
	}

	@Test // GH-3279
	void readsFragmentsFromBeanFactory() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithCustomImplementation.class);

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Class<?> repositoryInterfaceType = ConfigWithCustomImplementation.RepositoryWithCustomImplementation.class;
		Mockito.when(repoConfig.getRepositoryInterface()).thenReturn(repositoryInterfaceType.getName());

		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.repositoryInformation(repoConfig,
				repoFactoryBean.getMergedBeanDefinition(), repoFactoryBean.getBeanFactory());

		assertThat(repositoryInformation.getFragments()).satisfiesExactly(fragment -> {
			assertThat(fragment.getSignatureContributor())
					.isEqualTo(ConfigWithCustomImplementation.CustomImplInterface.class);
		});
	}

	@Test // GH-3279
	void fallsBackToModuleBaseClassIfSetAndNoRepoBaseDefined() {

		RegisteredBean repoFactoryBean = repositoryFactory(ConfigWithSimpleCrudRepository.class);
		RootBeanDefinition rootBeanDefinition = repoFactoryBean.getMergedBeanDefinition().cloneBeanDefinition();
		// need to unset because its defined as non default
		rootBeanDefinition.getPropertyValues().removePropertyValue("repositoryBaseClass");
		rootBeanDefinition.getPropertyValues().add("moduleBaseClass", ModuleBase.class.getName());

		RepositoryConfiguration<?> repoConfig = mock(RepositoryConfiguration.class);
		Mockito.when(repoConfig.getRepositoryInterface()).thenReturn(ConfigWithSimpleCrudRepository.MyRepo.class.getName());

		RepositoryInformation repositoryInformation = RepositoryBeanDefinitionReader.repositoryInformation(repoConfig,
				rootBeanDefinition, repoFactoryBean.getBeanFactory());

		assertThat(repositoryInformation.getRepositoryBaseClass()).isEqualTo(ModuleBase.class);
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

	static class ModuleBase {}
}
