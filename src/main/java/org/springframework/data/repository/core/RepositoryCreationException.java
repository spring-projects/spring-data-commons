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
package org.springframework.data.repository.core;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Exception thrown in the context of repository creation.
 *
 * @author Mark Paluch
 * @since 2.5
 */
@SuppressWarnings("serial")
public class RepositoryCreationException extends InvalidDataAccessApiUsageException {

	private final Class<?> repositoryInterface;

	/**
	 * Constructor for RepositoryCreationException.
	 *
	 * @param msg the detail message.
	 * @param repositoryInterface the repository interface.
	 */
	public RepositoryCreationException(String msg, Class<?> repositoryInterface) {
		super(msg);
		this.repositoryInterface = repositoryInterface;
	}

	/**
	 * Constructor for RepositoryException.
	 *
	 * @param msg the detail message.
	 * @param cause the root cause from the data access API in use.
	 * @param repositoryInterface the repository interface.
	 */
	public RepositoryCreationException(String msg, Throwable cause, Class<?> repositoryInterface) {
		super(msg, cause);
		this.repositoryInterface = repositoryInterface;
	}

	public Class<?> getRepositoryInterface() {
		return repositoryInterface;
	}
}
