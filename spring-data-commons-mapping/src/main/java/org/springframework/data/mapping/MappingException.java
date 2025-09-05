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
package org.springframework.data.mapping;

import java.io.Serial;

import org.jspecify.annotations.Nullable;

/**
 * @author Jon Brisbin
 */
public class MappingException extends RuntimeException {

	private static final @Serial long serialVersionUID = 1L;

	public MappingException(@Nullable String s) {
		super(s);
	}

	public MappingException(@Nullable String s, @Nullable Throwable throwable) {
		super(s, throwable);
	}
}
