/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.repository.core;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 3.4.0
 */
public class RepositoryMethodContextHolder {

	private static ContextProvider contextSupplier;

	static {
		contextSupplier = new ThreadLocalContextProvider();
	}

	@Nullable
	public static RepositoryMethodContext setContext(@Nullable RepositoryMethodContext context) {
		return contextSupplier.set(context);
	}

	@Nullable
	public static RepositoryMethodContext current() {
		return contextSupplier.get();
	}

	public static void clearContext() {
		contextSupplier.clear();
	}

	interface ContextProvider {

		@Nullable
		RepositoryMethodContext get();

		@Nullable
		RepositoryMethodContext set(@Nullable RepositoryMethodContext context);

		void clear();
	}

	static class ThreadLocalContextProvider implements ContextProvider {

		/**
		 * ThreadLocal holder for repository method associated with this thread. Will contain {@code null} unless the
		 * "exposeMetadata" property on the controlling repository factory configuration has been set to "true".
		 */
		private static final ThreadLocal<RepositoryMethodContext> currentMethod = new NamedThreadLocal<>(
				"Current Repository Method");

		@Override
		@Nullable
		public RepositoryMethodContext get() {
			return currentMethod.get();
		}

		public void clear() {
			currentMethod.remove();
		}

		@Override
		@Nullable
		public RepositoryMethodContext set(@Nullable RepositoryMethodContext context) {

			RepositoryMethodContext old = currentMethod.get();

			if (context != null) {
				currentMethod.set(context);
			} else {
				currentMethod.remove();
			}

			return old;
		}
	}
}
