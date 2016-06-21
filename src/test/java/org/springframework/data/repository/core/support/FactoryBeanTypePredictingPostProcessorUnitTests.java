/*
 * Copyright 2008-2016 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

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
import org.springframework.jndi.JndiObjectFactoryBean;

/**
 * Unit tests for {@link RepositoryInterfaceAwareBeanPostProcessor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class FactoryBeanTypePredictingPostProcessorUnitTests {

	private static final Class<?> FACTORY_CLASS = RepositoryFactoryBeanSupport.class;
	private static final String BEAN_NAME = "foo";
	private static final String DAO_INTERFACE_PROPERTY = "repositoryInterface";

	FactoryBeanTypePredictingBeanPostProcessor processor;
	BeanDefinition beanDefinition;

	@Mock ConfigurableListableBeanFactory beanFactory;

	@Before
	public void setUp() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FACTORY_CLASS)
				.addPropertyValue(DAO_INTERFACE_PROPERTY, UserDao.class);
		this.beanDefinition = builder.getBeanDefinition();
		this.processor = new FactoryBeanTypePredictingBeanPostProcessor(FACTORY_CLASS, "repositoryInterface");

		when(beanFactory.getBeanDefinition(BEAN_NAME)).thenReturn(beanDefinition);
	}

	@Test
	public void returnsDaoInterfaceClassForFactoryBean() throws Exception {

		processor.setBeanFactory(beanFactory);
		assertThat(processor.predictBeanType(FACTORY_CLASS, BEAN_NAME)).isEqualTo(UserDao.class);
	}

	@Test
	public void doesNotResolveInterfaceForNonFactoryClasses() throws Exception {

		processor.setBeanFactory(beanFactory);
		assertNotTypeDetected(BeanFactory.class);
	}

	@Test
	public void doesNotResolveInterfaceForUnloadableClass() throws Exception {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FACTORY_CLASS)
				.addPropertyValue(DAO_INTERFACE_PROPERTY, "com.acme.Foo");

		when(beanFactory.getBeanDefinition(BEAN_NAME)).thenReturn(builder.getBeanDefinition());
		processor.setBeanFactory(beanFactory);

		assertNotTypeDetected(FACTORY_CLASS);
	}

	@Test
	public void doesNotResolveTypeForPlainBeanFactory() throws Exception {

		BeanFactory beanFactory = mock(BeanFactory.class);
		processor.setBeanFactory(beanFactory);

		assertNotTypeDetected(FACTORY_CLASS);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNonFactoryBeanType() {
		new FactoryBeanTypePredictingBeanPostProcessor(Object.class, "property");
	}

	/**
	 * @see DATACMNS-821
	 */
	@Test
	public void usesFirstValueIfPropertyIsOfArrayType() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(JndiObjectFactoryBean.class);
		builder.addPropertyValue("proxyInterfaces",
				new String[] { Serializable.class.getName(), Iterable.class.getName() });

		when(beanFactory.getBeanDefinition(BEAN_NAME)).thenReturn(builder.getBeanDefinition());

		processor = new FactoryBeanTypePredictingBeanPostProcessor(JndiObjectFactoryBean.class, "proxyInterface",
				"proxyInterfaces");
		processor.setBeanFactory(beanFactory);

		assertThat(processor.predictBeanType(JndiObjectFactoryBean.class, BEAN_NAME)).isEqualTo(Serializable.class);
	}

	private void assertNotTypeDetected(Class<?> beanClass) {
		assertThat(processor.predictBeanType(beanClass, BEAN_NAME)).isNull();
	}

	private class User {}

	private interface UserDao extends Repository<User, Long> {}
}
