/*
 * Copyright 2024-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.data.test.util.ClassPathExclusions;

/**
 * Unit tests for {@link WebRuntimeHints}.
 *
 * @author Christoph Strobl
 */
class WebRuntimeHintsUnitTests {

	@Test // GH-3033
	void shouldRegisterRuntimeHintWhenJacksonPresent() {

		ReflectionHints reflectionHints = new ReflectionHints();
		RuntimeHints runtimeHints = mock(RuntimeHints.class);
		when(runtimeHints.reflection()).thenReturn(reflectionHints);

		new WebRuntimeHints().registerHints(runtimeHints, this.getClass().getClassLoader());

		assertThat(runtimeHints).matches(
				RuntimeHintsPredicates.reflection().onType(TypeReference.of("org.springframework.data.domain.Unpaged")));
	}

	@Test // GH-3033, GH-3171
	@ClassPathExclusions(packages = { "com.fasterxml.jackson.databind" })
	void shouldRegisterRuntimeHintWithTypeNameWhenJackson2NotPresent() {

		ReflectionHints reflectionHints = new ReflectionHints();
		RuntimeHints runtimeHints = mock(RuntimeHints.class);
		when(runtimeHints.reflection()).thenReturn(reflectionHints);

		new WebRuntimeHints().registerHints(runtimeHints, this.getClass().getClassLoader());

		assertThat(runtimeHints).matches(RuntimeHintsPredicates.reflection()
				.onType(TypeReference.of("org.springframework.data.web.config.SpringDataWebSettings")));
	}

	@Test // GH-3292
	@ClassPathExclusions(packages = { "tools.jackson" })
	void shouldRegisterRuntimeHintWithTypeNameWhenJackson3NotPresent() {

		ReflectionHints reflectionHints = new ReflectionHints();
		RuntimeHints runtimeHints = mock(RuntimeHints.class);
		when(runtimeHints.reflection()).thenReturn(reflectionHints);

		new WebRuntimeHints().registerHints(runtimeHints, this.getClass().getClassLoader());

		assertThat(runtimeHints).matches(RuntimeHintsPredicates.reflection()
				.onType(TypeReference.of("org.springframework.data.web.config.SpringDataWebSettings")));
	}
}
