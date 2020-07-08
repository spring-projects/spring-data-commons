/*
 * Copyright 2020 the original author or authors.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by repository method listeners. Listeners are notified with the called {@link Method},
 * arguments and its outcome.
 *
 * @author Mark Paluch
 * @since 2.4
 */
interface RepositoryInvocationListener {

	/**
	 * The repository method invocation to be handled after calling it.
	 *
	 * @param method
	 * @param args
	 * @param result
	 * @param exception
	 */
	void afterInvocation(Method method, Object[] args, @Nullable Object result, @Nullable Throwable exception);

	/**
	 * {@link RepositoryInvocationListener} that does nothing upon invocation.
	 *
	 * @author Mark Paluch
	 */
	enum NoOpRepositoryInvocationListener implements RepositoryInvocationListener {

		INSTANCE;

		@Override
		public void afterInvocation(Method method, Object[] args, @Nullable Object result, @Nullable Throwable exception) {

		}
	}

	/**
	 * {@link RepositoryInvocationListener} implementation that notifies {@link RepositoryMethodInvocationListener} upon
	 * {@link #afterInvocation(Method, Object[], Object, Throwable)}.
	 *
	 * @author Mark Paluch
	 */
	class RepositoryInvocationMulticaster implements RepositoryInvocationListener {

		private final Class<?> repositoryInterface;
		private final List<RepositoryMethodInvocationListener> methodInvocationListeners;
		private final long startNs;

		RepositoryInvocationMulticaster(Class<?> repositoryInterface,
				List<RepositoryMethodInvocationListener> methodInvocationListeners) {

			this.repositoryInterface = repositoryInterface;
			this.methodInvocationListeners = methodInvocationListeners;
			this.startNs = System.nanoTime();
		}

		@Override
		public void afterInvocation(Method method, Object[] args, @Nullable Object result, @Nullable Throwable exception) {

			long durationNs = getDuration(System.nanoTime());
			RepositoryMethodInvocationListener.Invocation invocation = new RepositoryMethodInvocationListener.Invocation(
					durationNs, repositoryInterface, method, result,
					exception instanceof InvocationTargetException ? exception.getCause() : exception);

			for (RepositoryMethodInvocationListener methodInvocationListener : methodInvocationListeners) {
				methodInvocationListener.afterInvocation(invocation);
			}
		}

		private long getDuration(long endNs) {

			if (endNs > startNs) {
				return endNs - startNs;
			}

			// end time overflow
			return (Long.MAX_VALUE - startNs) + endNs;
		}
	}
}
