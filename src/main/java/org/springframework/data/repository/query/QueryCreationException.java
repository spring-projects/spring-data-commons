/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.repository.core.RepositoryCreationException;

/**
 * Exception to be thrown if a query cannot be created from a {@link Method}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public final class QueryCreationException extends RepositoryCreationException {

	private static final long serialVersionUID = -1238456123580L;
	private static final String MESSAGE_TEMPLATE = "Could not create query for method %s! Could not find property %s on domain class %s.";

	private final Method method;

	/**
	 * Creates a new {@link QueryCreationException}.
	 */
	private QueryCreationException(String message, QueryMethod method) {

		super(message, method.getMetadata().getRepositoryInterface());
		this.method = method.getMethod();
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 */
	private QueryCreationException(String message, Throwable cause, Class<?> repositoryInterface, Method method) {

		super(message, cause, repositoryInterface);
		this.method = method;
	}

	/**
	 * Rejects the given domain class property.
	 *
	 * @param method
	 * @param propertyName
	 * @return
	 */
	public static QueryCreationException invalidProperty(QueryMethod method, String propertyName) {

		return new QueryCreationException(String.format(MESSAGE_TEMPLATE, method, propertyName, method.getDomainClass()
				.getName()), method);
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param method
	 * @param message
	 * @return
	 */
	public static QueryCreationException create(QueryMethod method, String message) {

		return new QueryCreationException(String.format("Could not create query for %s! Reason: %s", method, message),
				method);
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod} and {@link Throwable} as cause.
	 *
	 * @param method
	 * @param cause
	 * @return
	 */
	public static QueryCreationException create(QueryMethod method, Throwable cause) {
		return new QueryCreationException(cause.getMessage(), cause, method.getMetadata().getRepositoryInterface(),
				method.getMethod());
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod} and {@link Throwable} as cause.
	 *
	 * @param method
	 * @param cause
	 * @return
	 * @since 2.5
	 */
	public static QueryCreationException create(String message, Throwable cause, Class<?> repositoryInterface,
			Method method) {
		return new QueryCreationException(String.format("Could not create query for %s! Reason: %s", method, message),
				cause, repositoryInterface, method);
	}

	/**
	 * @return
	 * @since 2.5
	 */
	public Method getMethod() {
		return method;
	}
}
