/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.repository.core.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryInterfaceAwareBeanPostProcessor;

/**
 * Unit tests for {@link RepositoryInterfaceAwareBeanPostProcessor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryInterfaceAwareBeanPostProcessorUnitTests {

	private static final Class<?> FACTORY_CLASS = RepositoryFactoryBeanSupport.class;
	private static final String BEAN_NAME = "foo";
	private static final String DAO_INTERFACE_PROPERTY = "repositoryInterface";

	private RepositoryInterfaceAwareBeanPostProcessor processor;

	@Mock
	private ConfigurableListableBeanFactory beanFactory;
	private BeanDefinition beanDefinition;

	@Before
	public void setUp() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FACTORY_CLASS).addPropertyValue(
				DAO_INTERFACE_PROPERTY, UserDao.class);
		this.beanDefinition = builder.getBeanDefinition();

		when(beanFactory.getBeanDefinition(BEAN_NAME)).thenReturn(beanDefinition);

		processor = new RepositoryInterfaceAwareBeanPostProcessor();

	}

	@Test
	public void returnsDaoInterfaceClassForFactoryBean() throws Exception {

		processor.setBeanFactory(beanFactory);
		assertEquals(UserDao.class, processor.predictBeanType(FACTORY_CLASS, BEAN_NAME));
	}

	@Test
	public void doesNotResolveInterfaceForNonFactoryClasses() throws Exception {

		processor.setBeanFactory(beanFactory);
		assertNotTypeDetected(BeanFactory.class);
	}

	@Test
	public void doesNotResolveInterfaceForUnloadableClass() throws Exception {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FACTORY_CLASS).addPropertyValue(
				DAO_INTERFACE_PROPERTY, "com.acme.Foo");

		when(beanFactory.getBeanDefinition(BEAN_NAME)).thenReturn(builder.getBeanDefinition());

		assertNotTypeDetected(FACTORY_CLASS);
	}

	@Test
	public void doesNotResolveTypeForPlainBeanFactory() throws Exception {

		BeanFactory beanFactory = mock(BeanFactory.class);
		processor.setBeanFactory(beanFactory);

		assertNotTypeDetected(FACTORY_CLASS);
	}

	private void assertNotTypeDetected(Class<?> beanClass) {

		assertThat(processor.predictBeanType(beanClass, BEAN_NAME), is(nullValue()));
	}

	private class User {

	}

	private interface UserDao extends Repository<User, Long> {

	}
}
