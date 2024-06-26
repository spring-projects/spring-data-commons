/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Nullability}.
 *
 * @author Mark Paluch
 */
class NullabilityUnitTests {

	@Test
	void shouldConsiderPrimitiveNullability() throws NoSuchMethodException {

		Method method = getClass().getDeclaredMethod("someMethod", Integer.TYPE);
		Nullability.MethodNullability methodNullability = Nullability.forMethod(method);

		// method return type
		assertThat(methodNullability.isDeclared()).isTrue();
		assertThat(methodNullability.isNullable()).isTrue();

		Nullability pn = methodNullability.forParameter(0);

		// method return type
		assertThat(pn.isDeclared()).isTrue();
		assertThat(pn.isNullable()).isFalse();
	}

	void someMethod(int i) {

	}
}
