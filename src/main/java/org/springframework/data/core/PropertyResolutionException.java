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
import org.springframework.core.NestedRuntimeException;

/**
 * Exception indicating errors during property reference or property path resolution. Thrown when a property cannot be
 * resolved.
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class PropertyResolutionException extends NestedRuntimeException {

	/**
	 * Construct a {@code PropertyResolutionException} with the specified detail message.
	 *
	 * @param msg the detail message.
	 */
	public PropertyResolutionException(@Nullable String msg) {
		super(msg);
	}

	/**
	 * Construct a {@code PropertyResolutionException} with the specified detail message and nested exception.
	 *
	 * @param msg the detail message.
	 * @param cause the nested exception.
	 */
	public PropertyResolutionException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
