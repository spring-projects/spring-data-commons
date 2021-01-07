/*
 * Copyright 2018-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link ImplementationDetectionConfiguration}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
class ImplementationDetectionConfigurationUnitTests {

	@Test // DATACMNS-1439
	void shouldConsiderBeanNameDecapitalization() {

		assertThat(getBeanName("com.acme.UDPRepository")).isEqualTo("UDPRepository");
		assertThat(getBeanName("com.acme.UdpRepository")).isEqualTo("udpRepository");
	}

	private static String getBeanName(String className) {

		MockImplementationDetectionConfiguration configuration = new MockImplementationDetectionConfiguration();

		return configuration.generateBeanName(BeanDefinitionBuilder.rootBeanDefinition(className).getBeanDefinition());
	}

	private static class MockImplementationDetectionConfiguration implements ImplementationDetectionConfiguration {

		@Override
		public String getImplementationPostfix() {
			return "Impl";
		}

		@Override
		public Streamable<String> getBasePackages() {
			return Streamable.empty();
		}

		@Override
		public Streamable<TypeFilter> getExcludeFilters() {
			return Streamable.empty();
		}

		@Override
		public MetadataReaderFactory getMetadataReaderFactory() {
			return new SimpleMetadataReaderFactory();
		}
	}
}
