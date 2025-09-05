/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Unit tests for {@link DefaultRepositoryConfiguration}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultRepositoryConfigurationUnitTests {

	@Mock RepositoryConfigurationSource source;

	RepositoryConfigurationExtension extension = new SimplerRepositoryConfigurationExtension("factory", "module");

	@BeforeEach
	void before() {
		when(source.getBootstrapMode()).thenReturn(BootstrapMode.DEFAULT);
	}

	@Test
	void supportsBasicConfiguration() {

		RepositoryConfiguration<RepositoryConfigurationSource> configuration = getConfiguration(source);

		assertThat(configuration.getConfigurationSource()).isEqualTo(source);
		assertThat(configuration.getRepositoryInterface()).isEqualTo("com.acme.MyRepository");
		assertThat(configuration.getQueryLookupStrategyKey()).isEqualTo(Key.CREATE_IF_NOT_FOUND);
		assertThat(configuration.isLazyInit()).isFalse();
	}

	@Test // DATACMNS-1018
	void usesExtensionFactoryBeanClassNameIfNoneDefinedInSource() {
		assertThat(getConfiguration(source).getRepositoryFactoryBeanClassName()).isEqualTo("factory");
	}

	@Test // DATACMNS-1018
	void prefersSourcesRepositoryFactoryBeanClass() {

		when(source.getRepositoryFactoryBeanClassName()).thenReturn(Optional.of("custom"));

		assertThat(getConfiguration(source).getRepositoryFactoryBeanClassName()).isEqualTo("custom");
	}

	private DefaultRepositoryConfiguration<RepositoryConfigurationSource> getConfiguration(
			RepositoryConfigurationSource source) {
		return getConfiguration(source, "com.acme.MyRepository");
	}

	private DefaultRepositoryConfiguration<RepositoryConfigurationSource> getConfiguration(
			RepositoryConfigurationSource source, String repositoryInterfaceName) {
		var beanDefinition = createBeanDefinition(repositoryInterfaceName);
		return new DefaultRepositoryConfiguration<>(source, beanDefinition, extension);
	}

	private static final class SimplerRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {
		private final String repositoryFactoryBeanClassName, modulePrefix;

		public SimplerRepositoryConfigurationExtension(String repositoryFactoryBeanClassName, String modulePrefix) {
			this.repositoryFactoryBeanClassName = repositoryFactoryBeanClassName;
			this.modulePrefix = modulePrefix;
		}

		@Override
		public String getRepositoryFactoryBeanClassName() {
			return this.repositoryFactoryBeanClassName;
		}

		@Override
		public String getModulePrefix() {
			return this.modulePrefix;
		}

	}

	private static RootBeanDefinition createBeanDefinition(String repositoryInterfaceName) {

		var beanDefinition = new RootBeanDefinition(repositoryInterfaceName);

		var constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addGenericArgumentValue(MyRepository.class);
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);

		return beanDefinition;
	}

	private interface NestedInterface {}
}
