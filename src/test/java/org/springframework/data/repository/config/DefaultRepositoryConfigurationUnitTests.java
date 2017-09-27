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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Unit tests for {@link DefaultRepositoryConfiguration}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRepositoryConfigurationUnitTests {

	@Mock RepositoryConfigurationSource source;

	@Test
	public void supportsBasicConfiguration() {

		RepositoryConfiguration<RepositoryConfigurationSource> configuration = new DefaultRepositoryConfiguration<RepositoryConfigurationSource>(
				source, new RootBeanDefinition("com.acme.MyRepository"));

		assertThat(configuration.getConfigurationSource(), is(source));
		assertThat(configuration.getImplementationBeanName(), is("myRepositoryImpl"));
		assertThat(configuration.getImplementationClassName(), is("MyRepositoryImpl"));
		assertThat(configuration.getRepositoryInterface(), is("com.acme.MyRepository"));
		assertThat(configuration.getQueryLookupStrategyKey(), is((Object) Key.CREATE_IF_NOT_FOUND));
		assertThat(configuration.isLazyInit(), is(false));
	}

	@Test // DATACMNS-1172
	public void limitsImplementationBasePackages() {

		Iterable<String> packages = getConfiguration(source, "com.acme.MyRepository").getImplementationBasePackages();

		assertThat(packages, hasItem("com.acme"));
	}

	@Test // DATACMNS-1172
	public void limitsImplementationBasePackagesOfNestedClass() {

		Iterable<String> packages = getConfiguration(source, MyRepository.class.getName()).getImplementationBasePackages();

		assertThat(packages, hasItem("org.springframework.data.repository.config"));
	}

	private DefaultRepositoryConfiguration<RepositoryConfigurationSource> getConfiguration(
			RepositoryConfigurationSource source, String repositoryInterfaceName) {

		RootBeanDefinition beanDefinition = createBeanDefinition(repositoryInterfaceName);

		return new DefaultRepositoryConfiguration<RepositoryConfigurationSource>(source, beanDefinition);
	}

	private static RootBeanDefinition createBeanDefinition(String repositoryInterfaceName) {

		RootBeanDefinition beanDefinition = new RootBeanDefinition(repositoryInterfaceName);

		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addGenericArgumentValue(MyRepository.class);
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);

		return beanDefinition;
	}

	private interface NestedInterface {}
}
