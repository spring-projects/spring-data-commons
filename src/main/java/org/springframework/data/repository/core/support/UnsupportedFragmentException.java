/*
 * Copyright 2021 the original author or authors.
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

import org.springframework.data.repository.core.RepositoryCreationException;

/**
 * Exception thrown during repository creation when a well-known fragment interface is not supported by the repository
 * factory.
 *
 * @author Mark Paluch
 * @since 2.5
 */
@SuppressWarnings("serial")
public class UnsupportedFragmentException extends RepositoryCreationException {

	private final Class<?> fragmentInterface;

	/**
	 * Constructor for UnsupportedFragmentException.
	 *
	 * @param msg the detail message.
	 * @param repositoryInterface the repository interface.
	 * @param fragmentInterface the offending fragment interface.
	 */
	public UnsupportedFragmentException(String msg, Class<?> repositoryInterface, Class<?> fragmentInterface) {
		super(msg, repositoryInterface);
		this.fragmentInterface = fragmentInterface;
	}

	public Class<?> getFragmentInterface() {
		return fragmentInterface;
	}
}
