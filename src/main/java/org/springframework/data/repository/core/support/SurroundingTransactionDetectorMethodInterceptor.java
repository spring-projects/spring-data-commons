/*
 * Copyright 2016-2018 the original author or authors.
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

import javax.annotation.Nullable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link MethodInterceptor} detecting whether a transaction is already running and exposing that fact via
 * {@link #isSurroundingTransactionActive()}. Useful in case subsequent interceptors might create transactions
 * themselves but downstream components have to find out whether there was one running before the call entered the
 * proxy.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.13
 * @soundtrack Hendrik Freischlader Trio - Openness (Openness)
 */
public enum SurroundingTransactionDetectorMethodInterceptor implements MethodInterceptor {

	INSTANCE;

	private final ThreadLocal<Boolean> SURROUNDING_TX_ACTIVE = new ThreadLocal<>();

	/**
	 * Returns whether a transaction was active before the method call entered the repository proxy.
	 *
	 * @return
	 */
	public boolean isSurroundingTransactionActive() {
		return Boolean.TRUE == SURROUNDING_TX_ACTIVE.get();
	}

	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Nullable
	@Override
	public Object invoke(@SuppressWarnings("null") MethodInvocation invocation) throws Throwable {

		SURROUNDING_TX_ACTIVE.set(TransactionSynchronizationManager.isActualTransactionActive());

		try {
			return invocation.proceed();
		} finally {
			SURROUNDING_TX_ACTIVE.remove();
		}
	}
}
