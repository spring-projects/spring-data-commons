/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.support.PersistenceExceptionTranslationInterceptor;

/**
 * Unit test for {@link TransactionalRepositoryProxyPostProcessor}.
 *
 * @author Oliver Gierke
 * @since 1.6
 */
@ExtendWith(MockitoExtension.class)
class PersistenceExceptionTranslationRepositoryProxyPostProcessorUnitTests {

	@Mock ListableBeanFactory beanFactory;
	@Mock ProxyFactory proxyFactory;

	@Test // DATACMNS-318
	void rejectsNullBeanFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PersistenceExceptionTranslationRepositoryProxyPostProcessor(null));
	}

	@Test // DATACMNS-318
	void setsUpBasicInstance() throws Exception {

		RepositoryProxyPostProcessor postProcessor = new PersistenceExceptionTranslationRepositoryProxyPostProcessor(
				beanFactory);

		postProcessor.postProcess(proxyFactory, null);

		verify(proxyFactory).addAdvice(isA(PersistenceExceptionTranslationInterceptor.class));
	}
}
