/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.cdi;

import static org.assertj.core.api.Assertions.*;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * CDI integration tests for composed repositories.
 * 
 * @author Mark Paluch
 */
public class RepositoryFragmentsIntegrationTests {

	private static SeContainer container;

	@BeforeClass
	public static void setUp() {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addPackages(ComposedRepository.class) //
				.initialize();
	}

	@Test // DATACMNS-1233
	public void shouldInvokeCustomImplementationLast() {

		ComposedRepository repository = getBean(ComposedRepository.class);
		ComposedRepositoryImpl customImplementation = getBean(ComposedRepositoryImpl.class);
		AnotherFragmentInterfaceImpl shadowed = getBean(AnotherFragmentInterfaceImpl.class);

		assertThat(repository.getShadowed()).isEqualTo(2);
		assertThat(customImplementation.getShadowed()).isEqualTo(1);
		assertThat(shadowed.getShadowed()).isEqualTo(2);
	}

	@Test // DATACMNS-1233
	public void shouldRespectInterfaceOrder() {

		ComposedRepository repository = getBean(ComposedRepository.class);
		FragmentInterfaceImpl fragment = getBean(FragmentInterfaceImpl.class);
		AnotherFragmentInterfaceImpl shadowed = getBean(AnotherFragmentInterfaceImpl.class);

		assertThat(repository.getPriority()).isEqualTo(1);
		assertThat(fragment.getPriority()).isEqualTo(1);
		assertThat(shadowed.getPriority()).isEqualTo(2);
	}

	protected <T> T getBean(Class<T> type) {
		return container.select(type).get();
	}

	@AfterClass
	public static void tearDown() {
		container.close();
	}
}
