/*
 * Copyright 2015-2016 the original author or authors.
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
import static org.springframework.test.util.ReflectionTestUtils.*;

import org.junit.Test;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Unit tests for {@link TransactionalRepositoryFactoryBeanSupport}.
 * 
 * @author Oliver Gierke
 * @soundtrack The Intersphere - Live in Mannheim
 */
public class TransactionRepositoryFactoryBeanSupportUnitTests {

	/**
	 * @see DATACMNS-656
	 */
	@Test
	public void disablesDefaultTransactionsIfConfigured() {

		SampleTransactionalRepositoryFactoryBean factoryBean = new SampleTransactionalRepositoryFactoryBean();
		factoryBean.setEnableDefaultTransactions(false);
		factoryBean.setBeanFactory(new DefaultListableBeanFactory());
		factoryBean.afterPropertiesSet();

		CrudRepository<Object, Long> repository = factoryBean.getObject();

		Advisor[] advisors = ((Advised) repository).getAdvisors();
		boolean found = false;

		for (Advisor advisor : advisors) {

			if (advisor.getAdvice() instanceof TransactionInterceptor) {

				found = true;

				TransactionInterceptor interceptor = (TransactionInterceptor) advisor.getAdvice();
				assertThat(getField(interceptor.getTransactionAttributeSource(), "enableDefaultTransactions")).isEqualTo(false);
				break;
			}
		}

		assertThat(found).isTrue();
	}

	/**
	 * @see DATACMNS-880
	 */
	@Test
	public void propagatesBeanFactoryToSuperClass() {

		SampleTransactionalRepositoryFactoryBean factoryBean = new SampleTransactionalRepositoryFactoryBean();
		factoryBean.setBeanFactory(new DefaultListableBeanFactory());

		assertThat(ReflectionTestUtils.getField(factoryBean, RepositoryFactoryBeanSupport.class, "beanFactory"))
				.isNotNull();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static class SampleTransactionalRepositoryFactoryBean
			extends TransactionalRepositoryFactoryBeanSupport<CrudRepository<Object, Long>, Object, Long> {

		private final CrudRepository<Object, Long> repository = mock(CrudRepository.class);

		public SampleTransactionalRepositoryFactoryBean() {
			setRepositoryInterface((Class) CrudRepository.class);
		}

		@Override
		protected RepositoryFactorySupport doCreateRepositoryFactory() {
			return new DummyRepositoryFactory(repository);
		}
	}
}
