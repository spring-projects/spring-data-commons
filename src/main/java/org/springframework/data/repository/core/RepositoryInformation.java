/*
 * Copyright 2011-2025 the original author or authors.
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
import java.util.List;

import org.springframework.data.repository.core.support.RepositoryComposition;

/**
 * Additional repository specific information
 *
 * @author Oliver Gierke
 */
public interface RepositoryInformation extends RepositoryMetadata {

	/**
	 * Returns whether the given method is logically a base class method. This also includes methods (re)declared in the
	 * repository interface that match the signatures of the base implementation.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	boolean isBaseClassMethod(Method method);

	/**
	 * Returns whether the given method is a custom repository method.
	 *
	 * @param method
	 * @return
	 */
	boolean isCustomMethod(Method method);

	/**
	 * Returns whether the given method is a query method.
	 *
	 * @param method
	 * @return
	 */
	boolean isQueryMethod(Method method);

	/**
	 * Returns all methods considered to be query methods.
	 *
	 * @return
	 */
	List<Method> getQueryMethods();

	/**
	 * Returns the base class to be used to create the proxy backing instance.
	 *
	 * @return
	 */
	Class<?> getRepositoryBaseClass();

	/**
	 * Returns the target class method that is backing the given method. This can be necessary if a repository interface
	 * redeclares a method of the core repository interface (e.g. for transaction behavior customization). Returns the
	 * method itself if the target class does not implement the given method. Implementations need to make sure the
	 * {@link Method} returned can be invoked via reflection, i.e. needs to be accessible.
	 *
	 * @param method must not be {@literal null}.
	 * @return
	 */
	Method getTargetClassMethod(Method method);

	/**
	 * Returns if the configured repository interface has custom methods, that might have to be delegated to a custom
	 * implementation. This is used to verify repository configuration.
	 *
	 * @return
	 */
	default boolean hasCustomMethod() {
		return getQueryMethods().stream().anyMatch(this::isCustomMethod);
	}

	/**
	 * Returns whether the configured repository interface has query methods at all.
	 *
	 * @return
	 */
	default boolean hasQueryMethods() {
		return getQueryMethods().iterator().hasNext();
	}

	/**
	 * Returns the {@link RepositoryComposition} for this repository. This is used to determine the composition of the
	 * repository and its fragments.
	 *
	 * @return never {@literal null}.
	 * @since 4.0
	 */
	RepositoryComposition getRepositoryComposition();

}
