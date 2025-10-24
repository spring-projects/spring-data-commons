/*
 * Copyright 2008-2025 the original author or authors.
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

import java.io.Serial;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.data.repository.core.RepositoryCreationException;

/**
 * Exception to be thrown if a query cannot be created from a {@link Method}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public final class QueryCreationException extends RepositoryCreationException {

	private static final @Serial long serialVersionUID = -1238456123580L;

	private final Method method;

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param message the detail message.
	 * @param method the query method causing the exception.
	 */
	private QueryCreationException(String message, QueryMethod method) {

		super(message, method.getMetadata().getRepositoryInterface());
		this.method = method.getMethod();
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param message the detail message.
	 * @param cause the root cause from the data access API in use.
	 * @param method the query method causing the exception.
	 * @since 4.0
	 */
	private QueryCreationException(String message, @Nullable Throwable cause, QueryMethod method) {

		super(message, cause, method.getMetadata().getRepositoryInterface());
		this.method = method.getMethod();
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 */
	private QueryCreationException(@Nullable String message, @Nullable Throwable cause, Class<?> repositoryInterface,
			Method method) {

		super(message, cause, repositoryInterface);
		this.method = method;
	}

	/**
	 * Rejects the given domain class property.
	 *
	 * @param method the {@link QueryMethod} to create the exception for.
	 * @param propertyName the property name that could not be found.
	 * @return the {@link QueryCreationException}.
	 */
	public static QueryCreationException invalidProperty(QueryMethod method, String propertyName) {

		return new QueryCreationException(
				"Could not find property '%s' on domain class '%s'".formatted(propertyName, method.getDomainClass().getName()),
				method);
	}

	/**
	 * Creates a new {@link QueryCreationException}.
	 *
	 * @param method the {@link QueryMethod} to create the exception for.
	 * @param message the message to use.
	 * @return the {@link QueryCreationException}.
	 */
	public static QueryCreationException create(QueryMethod method, String message) {
		return new QueryCreationException(message, method);
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod} and {@link Throwable} as cause.
	 *
	 * @param method the query method causing the exception.
	 * @param cause the root cause from the data access API in use.
	 * @return the {@link QueryCreationException}.
	 */
	@SuppressWarnings("NullAway")
	public static QueryCreationException create(QueryMethod method, Throwable cause) {
		return new QueryCreationException(cause.getMessage(), cause, method);
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod}, {@code message} and
	 * {@link Throwable} as cause.
	 *
	 * @param method the query method causing the exception.
	 * @param message the detail message.
	 * @param cause the root cause from the data access API in use.
	 * @return the {@link QueryCreationException}.
	 * @since 4.0
	 */
	@SuppressWarnings("NullAway")
	public static QueryCreationException create(QueryMethod method, String message, Throwable cause) {
		return create(message, cause, method.getMetadata().getRepositoryInterface(), method.getMethod());
	}

	/**
	 * Creates a new {@link QueryCreationException} for the given {@link QueryMethod} and {@link Throwable} as cause.
	 *
	 * @param message the detail message.
	 * @param cause the root cause from the data access API in use.
	 * @param repositoryInterface the repository interface.
	 * @param method the query method causing the exception.
	 * @return the {@link QueryCreationException}.
	 * @since 2.5
	 */
	public static QueryCreationException create(@Nullable String message, @Nullable Throwable cause,
			Class<?> repositoryInterface, Method method) {
		return new QueryCreationException(message,
				cause, repositoryInterface, method);
	}


	/**
	 * @return the method causing the exception.
	 * @since 2.5
	 */
	public Method getMethod() {
		return method;
	}

	@Override
	public String getLocalizedMessage() {

		StringBuilder sb = new StringBuilder();
		sb.append(method.getDeclaringClass().getSimpleName()).append('.');
		sb.append(method.getName());

		sb.append(method.getName());
		sb.append(Arrays.stream(method.getParameterTypes()) //
				.map(Type::getTypeName) //
				.collect(Collectors.joining(",", "(", ")")));

		return "Cannot create query for method [%s]; %s".formatted(sb.toString(), getMessage());
	}

}
