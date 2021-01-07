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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Interface to be implemented by listeners that want to be notified upon repository method invocation. Listeners are
 * notified with an {@link RepositoryMethodInvocation} object that describes which repository method was invoked along
 * with invocation arguments and the call duration.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.4
 */
public interface RepositoryMethodInvocationListener {

	/**
	 * Handle the invocation event. This method is called after the execution has finished.
	 *
	 * @param repositoryMethodInvocation the invocation to respond to.
	 */
	void afterInvocation(RepositoryMethodInvocation repositoryMethodInvocation);

	/**
	 * Value object capturing the actual invocation.
	 */
	class RepositoryMethodInvocation {

		private final long durationNs;
		private final Class<?> repositoryInterface;
		private final Method method;
		private final RepositoryMethodInvocationResult result;

		/**
		 * @param repositoryInterface the repository interface that was used to call {@link Method}.
		 * @param method the actual method that was called.
		 * @param result the outcome of the invocation. Must not be {@literal null}.
		 * @param durationNs the duration in {@link TimeUnit#NANOSECONDS}.
		 */
		public RepositoryMethodInvocation(Class<?> repositoryInterface, Method method,
				RepositoryMethodInvocationResult result, long durationNs) {

			this.durationNs = durationNs;
			this.repositoryInterface = repositoryInterface;
			this.method = method;
			this.result = result;
		}

		public long getDuration(TimeUnit timeUnit) {

			Assert.notNull(timeUnit, "TimeUnit must not be null");

			return timeUnit.convert(durationNs, TimeUnit.NANOSECONDS);
		}

		public Class<?> getRepositoryInterface() {
			return repositoryInterface;
		}

		public Method getMethod() {
			return method;
		}

		@Nullable
		public RepositoryMethodInvocationResult getResult() {
			return result;
		}

		@Override
		public String toString() {

			return String.format("Invocation %s.%s(%s): %s ms - %s", repositoryInterface.getSimpleName(), method.getName(),
					StringUtils.arrayToCommaDelimitedString(
							Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toArray()),
					getDuration(TimeUnit.MILLISECONDS), result.getState());
		}
	}

	interface RepositoryMethodInvocationResult {

		State getState();

		@Nullable
		Throwable getError();

		public enum State {
			SUCCESS, ERROR, CANCELED, RUNNING
		}
	}
}
