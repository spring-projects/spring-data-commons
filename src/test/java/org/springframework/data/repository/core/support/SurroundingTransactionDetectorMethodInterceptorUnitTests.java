/*
 * Copyright 2016-2021 the original author or authors.
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
import static org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor.*;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Unit tests for {@link SurroundingTransactionDetectorMethodInterceptor}.
 *
 * @author Oliver Gierke
 * @soundtrack Hendrik Freischlader Trio - Openness (Openness)
 */
class SurroundingTransactionDetectorMethodInterceptorUnitTests {

	@Test // DATACMNS-959
	void registersActiveSurroundingTransaction() throws Throwable {

		TransactionSynchronizationManager.setActualTransactionActive(true);

		INSTANCE.invoke(new StubMethodInvocation(true));
	}

	@Test // DATACMNS-959
	void registersNoSurroundingTransaction() throws Throwable {

		TransactionSynchronizationManager.setActualTransactionActive(false);

		INSTANCE.invoke(new StubMethodInvocation(false));
	}

	static class StubMethodInvocation extends ReflectiveMethodInvocation {

		boolean transactionActive;

		StubMethodInvocation(boolean expectTransactionActive) throws Exception {
			super(null, null, Object.class.getMethod("toString"), null, null, null);
			this.transactionActive = expectTransactionActive;
		}

		/*
		 * (non-Javadoc)
		 * @see org.aopalliance.intercept.Joinpoint#proceed()
		 */
		public Object proceed() throws Throwable {

			assertThat(INSTANCE.isSurroundingTransactionActive()).isEqualTo(transactionActive);

			return null;
		}
	}
}
