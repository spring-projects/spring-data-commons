/*
 * Copyright 2013-2017 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.repository.Repository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RepositoryFactoryBeanSupport}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class RepositoryFactoryBeanSupportUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Test // DATACMNS-341
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setsConfiguredClassLoaderOnRepositoryFactory() {

		ClassLoader classLoader = mock(ClassLoader.class);

		RepositoryFactoryBeanSupport factoryBean = new DummyRepositoryFactoryBean(SampleRepository.class);
		factoryBean.setBeanClassLoader(classLoader);
		factoryBean.setLazyInit(true);
		factoryBean.afterPropertiesSet();

		Object factory = ReflectionTestUtils.getField(factoryBean, "factory");
		assertThat(ReflectionTestUtils.getField(factory, "classLoader"), is((Object) classLoader));
	}

	@Test // DATACMNS-432
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void initializationFailsWithMissingRepositoryInterface() {

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Repository interface");

		new DummyRepositoryFactoryBean(null);
	}

	interface SampleRepository extends Repository<Object, Long> {}
}
