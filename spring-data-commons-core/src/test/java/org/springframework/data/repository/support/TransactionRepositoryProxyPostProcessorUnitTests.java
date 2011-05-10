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
package org.springframework.data.repository.support;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.transaction.interceptor.TransactionInterceptor;


/**
 * Unit test for {@link TransactionalRepositoryProxyPostProcessor}.
 *
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionRepositoryProxyPostProcessorUnitTests {

	TransactionalRepositoryProxyPostProcessor processor;

	@Mock
	ListableBeanFactory beanFactory;
	@Mock
	ProxyFactory proxyFactory;


	@Before
	public void setUp() {

		Map<String, PersistenceExceptionTranslator> beans =
				new HashMap<String, PersistenceExceptionTranslator>();
		beans.put("foo", mock(PersistenceExceptionTranslator.class));
		when(
				beanFactory.getBeansOfType(
						eq(PersistenceExceptionTranslator.class), anyBoolean(),
						anyBoolean())).thenReturn(beans);
	}


	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullBeanFactory() throws Exception {

		new TransactionalRepositoryProxyPostProcessor(null, "transactionManager");
	}


	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullTxManagerName() throws Exception {

		new TransactionalRepositoryProxyPostProcessor(beanFactory, null);
	}


	@Test
	public void setsUpBasicInstance() throws Exception {

		RepositoryProxyPostProcessor postProcessor =
				new TransactionalRepositoryProxyPostProcessor(beanFactory, "txManager");

		postProcessor.postProcess(proxyFactory);

		verify(proxyFactory).addAdvice(
				isA(PersistenceExceptionTranslationInterceptor.class));
		verify(proxyFactory).addAdvice(isA(TransactionInterceptor.class));
	}
}
