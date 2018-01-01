/*
 * Copyright 2008-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.query;

/**
 * Exception to be thrown if a query cannot be created from a {@link QueryMethod}.
 *
 * @author Oliver Gierke
 */
public final class QueryCreationException extends RuntimeException {

	private static final long serialVersionUID = -1238456123580L;
	private static final String MESSAGE_TEMPLATE = "Could not create query for method %s! Could not find property %s on domain class %s.";

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param method
	 */
	private QueryCreationException(String message) {

		super(message);
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
				.getName()));
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param method
	 * @param message
	 * @return
	 */
	public static QueryCreationException create(QueryMethod method, String message) {

		return new QueryCreationException(String.format("Could not create query for %s! Reason: %s", method, message));
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod} and {@link Throwable} as cause.
	 *
	 * @param method
	 * @param cause
	 * @return
	 */
	public static QueryCreationException create(QueryMethod method, Throwable cause) {

		return create(method, cause.getMessage());
	}
}
