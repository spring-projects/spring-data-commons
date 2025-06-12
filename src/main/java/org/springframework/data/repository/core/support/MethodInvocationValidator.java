/*
 * Copyright 2017-2025 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.util.NullnessMethodInvocationValidator;

/**
 * Interceptor enforcing required return value and method parameter constraints declared on repository query methods.
 * Supports Kotlin nullability markers and JSR-305 Non-null annotations.
 *
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @author Christoph Strobl
 * @since 2.0
 * @see org.jspecify.annotations.NonNull
 * @see org.springframework.core.Nullness
 * @see org.springframework.data.util.ReflectionUtils#isNullable(org.springframework.core.MethodParameter)
 */
public class MethodInvocationValidator extends NullnessMethodInvocationValidator {

	@Override
	protected RuntimeException returnValueIsNull(Method method) {
		return new EmptyResultDataAccessException("Result must not be null", 1);
	}
}
