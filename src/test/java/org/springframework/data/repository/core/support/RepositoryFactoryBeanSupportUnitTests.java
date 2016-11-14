/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.springframework.data.repository.Repository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RepositoryFactoryBeanSupport}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class RepositoryFactoryBeanSupportUnitTests {

	/**
	 * @see DATACMNS-341
	 */
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setsConfiguredClassLoaderOnRepositoryFactory() {

		ClassLoader classLoader = mock(ClassLoader.class);

		RepositoryFactoryBeanSupport factoryBean = new DummyRepositoryFactoryBean();
		factoryBean.setBeanClassLoader(classLoader);
		factoryBean.setLazyInit(true);
		factoryBean.setRepositoryInterface(SampleRepository.class);
		factoryBean.afterPropertiesSet();

		Object factory = ReflectionTestUtils.getField(factoryBean, "factory");
		assertThat(ReflectionTestUtils.getField(factory, "classLoader")).isEqualTo(classLoader);
	}

	/**
	 * @see DATACMNS-432
	 */
	@Test
	@SuppressWarnings("rawtypes")
	public void initializationFailsWithMissingRepositoryInterface() {

		assertThatExceptionOfType(IllegalArgumentException.class)//
				.isThrownBy(() -> new DummyRepositoryFactoryBean().afterPropertiesSet())//
				.withMessageContaining("Repository interface");

	}

	interface SampleRepository extends Repository<Object, Long> {}
}
