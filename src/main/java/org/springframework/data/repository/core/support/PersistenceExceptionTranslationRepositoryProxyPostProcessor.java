/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.util.Assert;

/**
 * {@link RepositoryProxyPostProcessor} to register a {@link PersistenceExceptionTranslationInterceptor} on the
 * repository proxy.
 *
 * @author Oliver Gierke
 */
public class PersistenceExceptionTranslationRepositoryProxyPostProcessor implements RepositoryProxyPostProcessor {

	private final PersistenceExceptionTranslationInterceptor interceptor;

	/**
	 * Creates a new {@link PersistenceExceptionTranslationRepositoryProxyPostProcessor} using the given
	 * {@link ListableBeanFactory}.
	 *
	 * @param beanFactory must not be {@literal null}.
	 */
	public PersistenceExceptionTranslationRepositoryProxyPostProcessor(ListableBeanFactory beanFactory) {

		Assert.notNull(beanFactory, "BeanFactory must not be null!");

		this.interceptor = new PersistenceExceptionTranslationInterceptor();
		this.interceptor.setBeanFactory(beanFactory);
		this.interceptor.afterPropertiesSet();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryProxyPostProcessor#postProcess(org.springframework.aop.framework.ProxyFactory, org.springframework.data.repository.core.RepositoryInformation)
	 */
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {
		factory.addAdvice(interceptor);
	}
}
