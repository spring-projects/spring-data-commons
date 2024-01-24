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
package org.springframework.data.web.aot;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.data.test.util.ClassPathExclusions;

/**
 * @author Christoph Strobl
 */
class WebRuntimeHintsUnitTests {

	@Test // GH-3033
	void shouldRegisterRuntimeHintWhenJacksonPresent() {

		ReflectionHints reflectionHints = new ReflectionHints();
		RuntimeHints runtimeHints = Mockito.mock(RuntimeHints.class);
		Mockito.when(runtimeHints.reflection()).thenReturn(reflectionHints);

		new WebRuntimeHints().registerHints(runtimeHints, this.getClass().getClassLoader());
	}

	@Test // GH-3033
	@ClassPathExclusions(packages = { "com.fasterxml.jackson.databind" })
	void shouldRegisterRuntimeHintWithTypeNameWhenJacksonNotPresent() {

		ReflectionHints reflectionHints = new ReflectionHints();
		RuntimeHints runtimeHints = Mockito.mock(RuntimeHints.class);
		Mockito.when(runtimeHints.reflection()).thenReturn(reflectionHints);

		new WebRuntimeHints().registerHints(runtimeHints, this.getClass().getClassLoader());
	}
}
