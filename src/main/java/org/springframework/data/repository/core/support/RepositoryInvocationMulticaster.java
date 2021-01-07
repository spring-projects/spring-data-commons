/*
 * Copyright 2020-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult;

/**
 * Interface to be implemented by repository method listeners. Listeners are notified with the called {@link Method},
 * arguments and its outcome.
 *
 * @author Mark Paluch
 * @since 2.4
 */
interface RepositoryInvocationMulticaster {

	/**
	 * The repository method invocation to be handled after calling it.
	 *
	 * @param method
	 * @param args
	 * @param result
	 */
	void notifyListeners(Method method, Object[] args, RepositoryMethodInvocation result);

	/**
	 * {@link RepositoryInvocationMulticaster} that does nothing upon invocation.
	 *
	 * @author Mark Paluch
	 */
	enum NoOpRepositoryInvocationMulticaster implements RepositoryInvocationMulticaster {

		INSTANCE;

		@Override
		public void notifyListeners(Method method, Object[] args, RepositoryMethodInvocation result) {

		}
	}

	/**
	 * {@link RepositoryInvocationMulticaster} implementation that notifies {@link RepositoryMethodInvocationListener}
	 * upon {@link #notifyListeners(Method, Object[], RepositoryMethodInvocationResult)}.
	 *
	 * @author Mark Paluch
	 */
	class DefaultRepositoryInvocationMulticaster implements RepositoryInvocationMulticaster {

		private final List<RepositoryMethodInvocationListener> methodInvocationListeners;

		DefaultRepositoryInvocationMulticaster(List<RepositoryMethodInvocationListener> methodInvocationListeners) {

			this.methodInvocationListeners = methodInvocationListeners;
		}

		@Override
		public void notifyListeners(Method method, Object[] args, RepositoryMethodInvocation result) {

			for (RepositoryMethodInvocationListener methodInvocationListener : methodInvocationListeners) {
				methodInvocationListener.afterInvocation(result);
			}
		}
	}
}
