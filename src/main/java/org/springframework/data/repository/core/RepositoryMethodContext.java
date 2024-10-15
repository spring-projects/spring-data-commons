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

import java.lang.reflect.Method;

/**
 * Interface containing methods and value objects to obtain information about the current repository method invocation.
 * <p>
 * The {@link #getMetadata()} method is usable if the repository factory is configured to expose the current repository
 * method metadata (not the default). It returns the invoked repository method. Target objects or advice can use this to
 * make advised calls.
 * <p>
 * Spring Data's framework does not expose method metadata by default, as there is a performance cost in doing so.
 * <p>
 * The functionality in this class might be used by a target object that needed access to resources on the invocation.
 * However, this approach should not be used when there is a reasonable alternative, as it makes application code
 * dependent on usage in particular.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Drotbohm
 * @since 3.4.0
 */
public interface RepositoryMethodContext {

	/**
	 * Returns the metadata for the repository.
	 *
	 * @return the repository metadata, will never be {@literal null}.
	 */
	RepositoryMetadata getMetadata();

	/**
	 * Returns the current method that is being invoked.
	 * <p>
	 * The method object represents the method as being invoked on the repository interface. It doesn't match the backing
	 * repository implementation in case the method invocation is delegated to an implementation method.
	 *
	 * @return the current method, will never be {@literal null}.
	 */
	Method getMethod();
}
