/*
 * Copyright 2011 the original author or authors.
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

import org.junit.Test;

/**
 * Common integration tests for Spring Data repository CDI extension.
 * 
 * @author Oliver Gierke
 */
public abstract class CdiRepositoryExtensionSupportIntegrationTests {

	@Test
	public void createsSpringDataRepositoryBean() {

		assertThat(getBean(SampleRepository.class)).isNotNull();

		RepositoryClient client = getBean(RepositoryClient.class);
		assertThat(client.repository).isNotNull();
	}

	/**
	 * @see DATACMNS-557
	 */
	@Test
	public void createsSpringDataRepositoryWithCustimImplBean() {

		assertThat(getBean(AnotherRepository.class)).isNotNull();

		RepositoryClient client = getBean(RepositoryClient.class);
		assertThat(client.anotherRepository).isNotNull();

		// this will always return 0 since it's a mock
		assertThat(client.anotherRepository.returnZero()).isEqualTo(0);
	}

	protected abstract <T> T getBean(Class<T> type);
}
