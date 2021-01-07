/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.spel.spi;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link Function}.
 *
 * @author Oliver Drotbohm
 */
class FunctionUnitTests {

	@Test // DATACMNS-1518
	void detectsVarArgsOverload() {

		Method method = ReflectionUtils.findMethod(Sample.class, "someMethod", String[].class);

		Function function = new Function(method, new Sample());

		TypeDescriptor stringDescriptor = TypeDescriptor.valueOf(String.class);

		assertThat(function.supports(Arrays.asList(stringDescriptor, stringDescriptor))).isTrue();
	}

	@Test // DATACMNS-1518
	void detectsObjectVarArgsOverload() {

		Method method = ReflectionUtils.findMethod(Sample.class, "onePlusObjectVarargs", String.class, Object[].class);

		Function function = new Function(method, new Sample());

		TypeDescriptor stringDescriptor = TypeDescriptor.valueOf(String.class);

		assertThat(function.supports(Arrays.asList(stringDescriptor, stringDescriptor))).isTrue();
	}

	class Sample {

		String someMethod(String... args) {
			return "result";
		}

		String onePlusObjectVarargs(String string, Object... args) {
			return null;
		}
	}
}
