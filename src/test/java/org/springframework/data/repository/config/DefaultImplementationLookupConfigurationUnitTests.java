/*
 * Copyright 2018-2019 the original author or authors.
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

import org.junit.Test;

/**
 * Unit tests for {@link DefaultImplementationLookupConfigurationUnitTests}.
 *
 * @author Mark Paluch
 */
public class DefaultImplementationLookupConfigurationUnitTests {

	@Test // DATACMNS-1439
	public void shouldConsiderBeanNameDecapitalization() {

		ImplementationDetectionConfiguration idcMock = mock(ImplementationDetectionConfiguration.class);
		when(idcMock.getImplementationPostfix()).thenReturn("Impl");

		assertThat(getImplementationBeanName(idcMock, "com.acme.UDPRepository")).isEqualTo("UDPRepositoryImpl");
		assertThat(getImplementationBeanName(idcMock, "com.acme.UdpRepository")).isEqualTo("udpRepositoryImpl");
	}

	private static String getImplementationBeanName(ImplementationDetectionConfiguration idcMock, String interfaceName) {

		DefaultImplementationLookupConfiguration configuration = new DefaultImplementationLookupConfiguration(idcMock,
				interfaceName);
		return configuration.getImplementationBeanName();
	}
}
