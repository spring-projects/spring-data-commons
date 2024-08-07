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
package org.springframework.data.repository.core.support;

import java.lang.reflect.Method;

import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.lang.Nullable;

/**
 * Interface containing methods and value objects to obtain information about the current repository method invocation.
 * <p>
 * The {@link #currentMethod()} method is usable if the repository factory is configured to expose the current
 * repository method metadata (not the default). It returns the invoked repository method. Target objects or advice can
 * use this to make advised calls.
 * <p>
 * Spring Data's framework does not expose method metadata by default, as there is a performance cost in doing so.
 * <p>
 * The functionality in this class might be used by a target object that needed access to resources on the invocation.
 * However, this approach should not be used when there is a reasonable alternative, as it makes application code
 * dependent on usage in particular.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.4.0
 */
public interface RepositoryMethodContext {

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
	static RepositoryMethodContext currentMethod() throws IllegalStateException {

		RepositoryMethodContext metadata = DefaultRepositoryMethodContext.getMetadata();
		if (metadata == null) {
			throw new IllegalStateException(
					"Cannot find current repository method: Set 'exposeMetadata' property on RepositoryFactorySupport to 'true' to make it available, and "
							+ "ensure that RepositoryMethodContext.currentMethod() is invoked in the same thread as the repository invocation.");
		}
		return metadata;
	}

	/**
	 * Make the given repository method metadata available via the {@link #currentMethod()} method.
	 * <p>
	 * Note that the caller should be careful to keep the old value as appropriate.
	 *
	 * @param metadata the metadata to expose (or {@code null} to reset it)
	 * @return the old metadata, which may be {@code null} if none was bound
	 * @see #currentMethod()
	 */
	@Nullable
	static RepositoryMethodContext setCurrentMetadata(@Nullable RepositoryMethodContext metadata) {
		return DefaultRepositoryMethodContext.setMetadata(metadata);
	}

	/**
	 * Returns the metadata for the repository.
	 *
	 * @return the repository metadata.
	 */
	RepositoryMetadata getRepository();

	/**
	 * Returns the current method that is being invoked.
	 * <p>
	 * The method object represents the method as being invoked on the repository interface. It doesn't match the backing
	 * repository implementation in case the method invocation is delegated to an implementation method.
	 *
	 * @return the current method.
	 */
	Method getMethod();

}
