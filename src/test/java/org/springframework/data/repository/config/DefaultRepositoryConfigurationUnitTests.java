/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;

/**
 * Unit tests for {@link DefaultRepositoryConfiguration}.
 * 
 * @author Oliver Gierke
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
}
