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

/**
 * Exception to be thrown when trying to access a {@link Parameter} with an invalid index inside a {@link Parameters}
 * instance.
 *
 * @author Oliver Gierke
 */
public class ParameterOutOfBoundsException extends RuntimeException {

	private static final @Serial long serialVersionUID = 8433209953653278886L;

	/**
	 * Creates a new {@link ParameterOutOfBoundsException} with the given exception as cause.
	 *
	 * @param message
	 * @param cause
	 */
	public ParameterOutOfBoundsException(String message, Throwable cause) {
		super(message, cause);
	}
}
