/*
 * Copyright 2018-2023 the original author or authors.
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

import java.beans.Introspector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link DefaultImplementationLookupConfigurationUnitTests}.
 *
 * @author Mark Paluch
 * @author Kyrylo Merzlikin
 */
class DefaultImplementationLookupConfigurationUnitTests {

	ImplementationDetectionConfiguration idcMock = mock(ImplementationDetectionConfiguration.class);

	@BeforeEach
	void setUp() {
		when(idcMock.getImplementationPostfix()).thenReturn("Impl");
		when(idcMock.forRepositoryConfiguration(any())).thenCallRealMethod();
		when(idcMock.forFragment(any())).thenCallRealMethod();
	}

	@Test // DATACMNS-1439
	void shouldConsiderBeanNameDecapitalization() {

		assertThat(getImplementationBeanName(idcMock, "com.acme.UDPRepository")).isEqualTo("UDPRepositoryImpl");
		assertThat(getImplementationBeanName(idcMock, "com.acme.UdpRepository")).isEqualTo("udpRepositoryImpl");
	}

	@Test // DATACMNS-1754
	void shouldUseSimpleClassNameWhenDefiningImplementationNames() {

		var lookupConfiguration = idcMock.forFragment("com.acme.Repositories$NestedRepository");
		assertThat(lookupConfiguration.getImplementationBeanName()).isEqualTo("repositories.NestedRepositoryImpl");
		assertThat(lookupConfiguration.getImplementationClassName()).isEqualTo("NestedRepositoryImpl");
	}

	private static String getImplementationBeanName(ImplementationDetectionConfiguration idcMock, String interfaceName) {

		var source = mock(RepositoryConfigurationSource.class);
		when(source.generateBeanName(any())).thenReturn(Introspector.decapitalize(ClassUtils.getShortName(interfaceName)));

		RepositoryConfiguration<?> repoConfig = new DefaultRepositoryConfiguration<>(source,
				BeanDefinitionBuilder.rootBeanDefinition(interfaceName).getBeanDefinition(), null);

		var configuration = idcMock.forRepositoryConfiguration(repoConfig);
		return configuration.getImplementationBeanName();
	}
}
