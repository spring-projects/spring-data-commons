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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Unit tests for {@link DefaultRepositoryConfiguration}.
 * 
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRepositoryConfigurationUnitTests {

	@Mock RepositoryConfigurationSource source;

	BeanDefinition definition = new RootBeanDefinition("com.acme.MyRepository");
	RepositoryConfigurationExtension extension = new SimplerRepositoryConfigurationExtension("factory", "module");

	@Before
	public void before() {

		RepositoryBeanNameGenerator generator = new RepositoryBeanNameGenerator(getClass().getClassLoader());
		Answer<Object> answer = invocation -> generator.generateBeanName((BeanDefinition) invocation.getArguments()[0]);
		when(source.generateBeanName(Mockito.any(BeanDefinition.class))).then(answer);
	}

	@Test
	public void supportsBasicConfiguration() {

		RepositoryConfiguration<RepositoryConfigurationSource> configuration = getConfiguration(source);

		assertThat(configuration.getConfigurationSource()).isEqualTo(source);
		assertThat(configuration.getImplementationBeanName()).isEqualTo("myRepositoryImpl");
		assertThat(configuration.getImplementationClassName()).isEqualTo("MyRepositoryImpl");
		assertThat(configuration.getRepositoryInterface()).isEqualTo("com.acme.MyRepository");
		assertThat(configuration.getQueryLookupStrategyKey()).isEqualTo(Key.CREATE_IF_NOT_FOUND);
		assertThat(configuration.isLazyInit()).isFalse();
	}

	@Test // DATACMNS-1018
	public void usesExtensionFactoryBeanClassNameIfNoneDefinedInSource() {
		assertThat(getConfiguration(source).getRepositoryFactoryBeanClassName()).isEqualTo("factory");
	}

	@Test // DATACMNS-1018
	public void prefersSourcesRepositoryFactoryBeanClass() {

		when(source.getRepositoryFactoryBeanClassName()).thenReturn(Optional.of("custom"));

		assertThat(getConfiguration(source).getRepositoryFactoryBeanClassName()).isEqualTo("custom");
	}

	private DefaultRepositoryConfiguration<RepositoryConfigurationSource> getConfiguration(
			RepositoryConfigurationSource source) {
		RootBeanDefinition beanDefinition = createBeanDefinition();
		return new DefaultRepositoryConfiguration<>(source, beanDefinition, extension);
	}

	@Value
	@EqualsAndHashCode(callSuper = true)
	private static class SimplerRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {
		String repositoryFactoryBeanClassName, modulePrefix;
	}

	private static RootBeanDefinition createBeanDefinition() {

		RootBeanDefinition beanDefinition = new RootBeanDefinition("com.acme.MyRepository");

		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addGenericArgumentValue(MyRepository.class);
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);

		return beanDefinition;
	}
}
