/*
 * Copyright 2011-2021 the original author or authors.
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
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor.RepositoryAnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Unit tests for {@link RepositoryAnnotationTransactionAttributeSource}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class RepositoryInformationPreferringAnnotationTransactionAttributeSourceUnitTests {

	@Test
	void usesCustomTransactionConfigurationOnInterface() throws Exception {

		RepositoryInformation information = mock(RepositoryInformation.class);

		RepositoryAnnotationTransactionAttributeSource source = new RepositoryAnnotationTransactionAttributeSource(
				information, true);

		TransactionAttribute attribute = source.getTransactionAttribute(Bar.class.getMethod("bar", Object.class),
				FooImpl.class);
		assertThat(attribute.isReadOnly()).isFalse();

		attribute = source.getTransactionAttribute(Bar.class.getMethod("foo"), FooImpl.class);
		assertThat(attribute.isReadOnly()).isFalse();
	}

	/**
	 * Basic interface.
	 *
	 * @author Oliver Gierke
	 */
	interface Foo<T> {

		void foo();

		void bar(T param);
	}

	/**
	 * Implementation defining transaction configuration.
	 *
	 * @author Oliver Gierke
	 */
	@Transactional(readOnly = true)
	class FooImpl implements Foo<Object> {

		@Transactional
		public void foo() {

		}

		public void bar(Object param) {

		}
	}

	/**
	 * Interface reconfiguring transactions.
	 *
	 * @author Oliver Gierke
	 */
	interface Bar extends Foo<Object> {

		@Transactional
		void bar(Object param);
	}
}
