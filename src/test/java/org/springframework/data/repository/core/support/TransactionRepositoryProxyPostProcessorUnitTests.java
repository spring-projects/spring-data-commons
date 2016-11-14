/*
 * Copyright 2008-2015 the original author or authors.
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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor.CustomAnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Unit test for {@link TransactionalRepositoryProxyPostProcessor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionRepositoryProxyPostProcessorUnitTests {

	@Mock ListableBeanFactory beanFactory;
	@Mock ProxyFactory proxyFactory;
	@Mock RepositoryInformation repositoryInformation;

	@Before
	public void setUp() {

		Map<String, PersistenceExceptionTranslator> beans = new HashMap<String, PersistenceExceptionTranslator>();
		beans.put("foo", mock(PersistenceExceptionTranslator.class));
		when(beanFactory.getBeansOfType(eq(PersistenceExceptionTranslator.class), anyBoolean(), anyBoolean()))
				.thenReturn(beans);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBeanFactory() throws Exception {
		new TransactionalRepositoryProxyPostProcessor(null, "transactionManager", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTxManagerName() throws Exception {
		new TransactionalRepositoryProxyPostProcessor(beanFactory, null, true);
	}

	@Test
	public void setsUpBasicInstance() throws Exception {

		RepositoryProxyPostProcessor postProcessor = new TransactionalRepositoryProxyPostProcessor(beanFactory, "txManager",
				true);
		postProcessor.postProcess(proxyFactory, repositoryInformation);

		verify(proxyFactory).addAdvice(Mockito.any(TransactionInterceptor.class));
	}

	/**
	 * @see DATACMNS-464
	 */
	@Test
	public void fallsBackToTargetMethodTransactionSettings() throws Exception {
		assertTransactionAttributeFor(SampleImplementation.class);
	}

	/**
	 * @see DATACMNS-464
	 */
	@Test
	public void fallsBackToTargetClassTransactionSettings() throws Exception {
		assertTransactionAttributeFor(SampleImplementationWithClassAnnotation.class);
	}

	/**
	 * @see DATACMNS-732
	 */
	@Test
	public void considersJtaTransactional() throws Exception {

		Method method = SampleRepository.class.getMethod("methodWithJtaOneDotTwoAtTransactional");

		TransactionAttributeSource attributeSource = new CustomAnnotationTransactionAttributeSource();
		TransactionAttribute attribute = attributeSource.getTransactionAttribute(method, SampleRepository.class);

		assertThat(attribute).isNotNull();
	}

	private void assertTransactionAttributeFor(Class<?> implementationClass) throws Exception {

		Method repositorySaveMethod = SampleRepository.class.getMethod("save", Sample.class);
		Method implementationClassMethod = implementationClass.getMethod("save", Object.class);

		when(repositoryInformation.getTargetClassMethod(repositorySaveMethod)).thenReturn(implementationClassMethod);

		CustomAnnotationTransactionAttributeSource attributeSource = new CustomAnnotationTransactionAttributeSource();
		attributeSource.setRepositoryInformation(repositoryInformation);

		TransactionAttribute attribute = attributeSource.getTransactionAttribute(repositorySaveMethod,
				SampleImplementation.class);

		assertThat(attribute).isNotNull();
	}

	static class Sample {}

	interface SampleRepository extends Repository<Sample, Serializable> {

		Sample save(Sample object);

		@javax.transaction.Transactional
		void methodWithJtaOneDotTwoAtTransactional();
	}

	static class SampleImplementation<T> {

		@Transactional
		public <S extends T> S save(S object) {
			return null;
		}
	}

	@Transactional
	static class SampleImplementationWithClassAnnotation<T> {

		public <S extends T> S save(S object) {
			return null;
		}
	}
}
