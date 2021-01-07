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
package org.springframework.data.repository.cdi;

import static org.assertj.core.api.Assertions.*;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.data.repository.cdi.isolated.IsolatedComposedRepository;

/**
 * CDI integration tests for {@link CdiRepositoryConfiguration}.
 *
 * @author Mark Paluch
 */
class CdiConfigurationIntegrationTests {

	private static SeContainer container;

	@BeforeAll
	static void setUp() {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addPackages(IsolatedComposedRepository.class) //
				.initialize();
	}

	@Test // DATACMNS-1233
	void shouldApplyImplementationPostfix() {

		IsolatedComposedRepository repository = container.select(IsolatedComposedRepository.class).get();

		assertThat(repository.getPriority()).isEqualTo(42);
	}

	@AfterAll
	static void tearDown() {
		container.close();
	}
}
