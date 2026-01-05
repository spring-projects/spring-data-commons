/*
 * Copyright 2024-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.NamedThreadLocal;

/**
 * Associates a given {@link RepositoryMethodContext} with the current execution thread.
 * <p>
 * This class provides a series of static methods that interact with a thread-local storage of
 * {@link RepositoryMethodContext}. The purpose of the class is to provide a convenient way to be used for an
 * application.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.4
 * @see RepositoryMethodContext
 */
public class RepositoryMethodContextHolder {

	/**
	 * ThreadLocal holder for repository method associated with this thread. Will contain {@code null} unless the
	 * "exposeMetadata" property on the controlling repository factory configuration has been set to {@code true}.
	 */
	private static final ThreadLocal<RepositoryMethodContext> currentMethod = new NamedThreadLocal<>(
			"Current Repository Method");

	/**
	 * Make the given repository method metadata available via the {@link #getContext()} method.
	 * <p>
	 * Note that the caller should be careful to keep the old value as appropriate.
	 *
	 * @param context the metadata to expose (or {@code null} to reset it)
	 * @return the old metadata, which may be {@code null} if none was bound
	 * @see #getContext()
	 */
	public static @Nullable RepositoryMethodContext setContext(@Nullable RepositoryMethodContext context) {

		RepositoryMethodContext old = currentMethod.get();
		if (context != null) {
			currentMethod.set(context);
		} else {
			currentMethod.remove();
		}

		return old;
	}

	/**
	 * Try to return the current repository method metadata. This method is usable only if the calling method has been
	 * invoked via a repository method, and the repository factory has been set to expose metadata. Otherwise, this method
	 * will throw an IllegalStateException.
	 *
	 * @return the current repository method metadata (never returns {@code null})
	 * @throws IllegalStateException if the repository method metadata cannot be found, because the method was invoked
	 *           outside a repository method invocation context, or because the repository has not been configured to
	 *           expose its metadata.
	 */
	public static RepositoryMethodContext getContext() {

		RepositoryMethodContext metadata = currentMethod.get();

		if (metadata == null) {
			throw new IllegalStateException(
					"Cannot find current repository method: Set 'exposeMetadata' property on RepositoryFactorySupport to 'true' to make it available, and "
							+ "ensure that RepositoryMethodContext.getContext() is invoked in the same thread as the repository invocation.");
		}

		return metadata;
	}
}
