/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.core;

import org.jspecify.annotations.Nullable;

/**
 * {@link RuntimeException} for errors during {@link java.lang.invoke.SerializedLambda} parsing or introspection of
 * types.
 * <p>
 * Typically, thrown when a lambda or method reference is not a serializable lambda, or violates supported patterns
 * (e.g. constructor references, multi-step access).
 *
 * @author Christoph Strobl
 * @since 4.1
 */
public class LambdaIntrospectionException extends PropertyResolutionException {

	/**
	 * Construct a {@code LambdaIntrospectionException} with the specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public LambdaIntrospectionException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code LambdaIntrospectionException} with the specified detail message and nested exception.
	 *
	 * @param msg the detail message.
	 * @param cause the nested exception.
	 */
	public LambdaIntrospectionException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
