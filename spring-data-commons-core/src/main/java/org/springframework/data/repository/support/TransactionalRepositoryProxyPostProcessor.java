/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.repository.support;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;


/**
 * {@link RepositoryProxyPostProcessor} to add transactional behaviour to
 * repository proxies. Adds a {@link PersistenceExceptionTranslationInterceptor}
 * as well as an annotation based {@link TransactionInterceptor} to the proxy.
 *
 * @author Oliver Gierke
 */
class TransactionalRepositoryProxyPostProcessor implements
		RepositoryProxyPostProcessor {

	private final TransactionInterceptor transactionInterceptor;
	private final PersistenceExceptionTranslationInterceptor petInterceptor;


	/**
	 * Creates a new {@link TransactionalRepositoryProxyPostProcessor}.
	 */
	public TransactionalRepositoryProxyPostProcessor(
			ListableBeanFactory beanFactory, String transactionManagerName) {

		Assert.notNull(beanFactory);
		Assert.notNull(transactionManagerName);

		this.petInterceptor = new PersistenceExceptionTranslationInterceptor();
		this.petInterceptor.setBeanFactory(beanFactory);
		this.petInterceptor.afterPropertiesSet();

		this.transactionInterceptor =
				new TransactionInterceptor(null,
						new AnnotationTransactionAttributeSource());
		this.transactionInterceptor
				.setTransactionManagerBeanName(transactionManagerName);
		this.transactionInterceptor.setBeanFactory(beanFactory);
		this.transactionInterceptor.afterPropertiesSet();
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.springframework.data.repository.support.RepositoryProxyPostProcessor
			 * #postProcess(org.springframework.aop.framework.ProxyFactory)
			 */
	public void postProcess(ProxyFactory factory) {

		factory.addAdvice(petInterceptor);
		factory.addAdvice(transactionInterceptor);
	}
}
