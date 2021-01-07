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

import java.io.Serializable;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

/**
 * CDI integration tests for composed repositories.
 *
 * @author Mark Paluch
 * @author Kyrylo Merzlikin
 */
class RepositoryFragmentsIntegrationTests {

	private static SeContainer container;

	@BeforeAll
	static void setUp() {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addPackages(ComposedRepository.class) //
				.initialize();
	}

	@Test // DATACMNS-1233
	void shouldInvokeCustomImplementationLast() {

		ComposedRepository repository = getBean(ComposedRepository.class);
		ComposedRepositoryImpl customImplementation = getBean(ComposedRepositoryImpl.class);
		AnotherFragmentInterfaceImpl shadowed = getBean(AnotherFragmentInterfaceImpl.class);

		assertThat(repository.getShadowed()).isEqualTo(2);
		assertThat(customImplementation.getShadowed()).isEqualTo(1);
		assertThat(shadowed.getShadowed()).isEqualTo(2);
	}

	@Test // DATACMNS-1233
	void shouldRespectInterfaceOrder() {

		ComposedRepository repository = getBean(ComposedRepository.class);
		FragmentInterfaceImpl fragment = getBean(FragmentInterfaceImpl.class);
		AnotherFragmentInterfaceImpl shadowed = getBean(AnotherFragmentInterfaceImpl.class);

		assertThat(repository.getPriority()).isEqualTo(1);
		assertThat(fragment.getPriority()).isEqualTo(1);
		assertThat(shadowed.getPriority()).isEqualTo(2);
	}

	@Test // DATACMNS-1754
	void shouldFindCustomImplementationForNestedRepository() {

		NestedRepository repository = getBean(NestedRepository.class);

		assertThat(repository.getCustom()).isEqualTo("CustomImpl");
	}

	@Test // DATACMNS-1754
	void shouldFindImplementationForNestedRepositoryFragment() {

		ComposedRepository repository = getBean(ComposedRepository.class);
		RepositoryFragments.NestedFragmentInterfaceImpl fragment = getBean(
				RepositoryFragments.NestedFragmentInterfaceImpl.class);

		assertThat(repository.getKey()).isEqualTo("NestedFragmentImpl");
		assertThat(fragment.getKey()).isEqualTo("NestedFragmentImpl");
	}

	protected <T> T getBean(Class<T> type) {
		return container.select(type).get();
	}

	@AfterAll
	static void tearDown() {
		container.close();
	}

	public interface NestedRepository extends Repository<Object, Serializable> {

		String getCustom();
	}

	public interface NestedRepositoryCustom {

		// duplicate method allowing to provide custom implementation (without modifying original repository)
		String getCustom();
	}

	public static class NestedRepositoryImpl implements NestedRepositoryCustom {

		@Override
		public String getCustom() {
			return "CustomImpl";
		}
	}
}
