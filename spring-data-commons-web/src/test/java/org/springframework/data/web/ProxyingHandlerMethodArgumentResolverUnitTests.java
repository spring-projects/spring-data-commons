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
package org.springframework.data.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import example.SampleInterface;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.log.LogAccessor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for {@link ProxyingHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 * @author Chris Bono
 * @author Mark Paluch
 * @soundtrack Karlijn Langendijk & Sönke Meinen - Englishman In New York (Sting,
 *             https://www.youtube.com/watch?v=O7LZsqrnaaA)
 */
class ProxyingHandlerMethodArgumentResolverUnitTests {

	ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(
			() -> new DefaultConversionService(), true);

	@Test // DATACMNS-776
	void supportAnnotatedInterface() {

		var parameter = getParameter("with", AnnotatedInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void supportsUnannotatedInterfaceFromUserPackage() {

		var parameter = getParameter("with", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void doesNotSupportUnannotatedInterfaceFromSpringNamespace() {

		var parameter = getParameter("with", UnannotatedInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // DATACMNS-776
	void doesNotSupportCoreJavaType() {

		var parameter = getParameter("with", List.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-2937
	void doesNotSupportForeignSpringAnnotations() {

		var parameter = getParameter("withForeignAnnotation", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-2937
	void doesSupportAtModelAttribute() {

		var parameter = getParameter("withModelAttribute", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // GH-3258
	void doesNotSupportAtModelAttributeForMultipartParam() {

		var parameter = getParameter("withModelAttributeMultipart", MultipartFile.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-3258
	void doesSupportAtProjectedPayload() {

		var parameter = getParameter("withProjectedPayload", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // GH-3258
	void doesNotSupportAtProjectedPayloadForMultipartParam() {

		var parameter = getParameter("withProjectedPayloadMultipart", MultipartFile.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-3300
	@SuppressWarnings("unchecked")
	void deprecationLoggerOnlyLogsOncePerParameter() {

		var parameter = getParameter("withModelAttribute", SampleInterface.class);

		// Spy on the actual logger
		var actualLoggerSpy = spy(new LogAccessor(ProxyingHandlerMethodArgumentResolver.class));
		ReflectionTestUtils.setField(ProxyingHandlerMethodArgumentResolver.class, "LOGGER", actualLoggerSpy,
				LogAccessor.class);

		// Invoke twice but should only log the first time
		assertThat(resolver.supportsParameter(parameter)).isTrue();
		verify(actualLoggerSpy, times(1)).warn(any(Supplier.class));
		assertThat(resolver.supportsParameter(parameter)).isTrue();
		verifyNoMoreInteractions(actualLoggerSpy);
	}

	@ParameterizedTest // GH-3300
	@ValueSource(strings = { "withProjectedPayload", "withAnnotatedInterface" })
	void shouldNotLogDeprecationForValidUsage(String methodName) {

		var parameter = getParameter(methodName);

		// Spy on the actual logger
		var actualLoggerSpy = spy(new LogAccessor(ProxyingHandlerMethodArgumentResolver.class));
		ReflectionTestUtils.setField(ProxyingHandlerMethodArgumentResolver.class, "LOGGER", actualLoggerSpy,
				LogAccessor.class);

		// Invoke twice but should only log the first time
		assertThat(resolver.supportsParameter(parameter)).isTrue();
		verifyNoInteractions(actualLoggerSpy);
	}

	private static MethodParameter getParameter(String methodName, Class<?> parameterType) {

		var method = ReflectionUtils.findMethod(Controller.class, methodName, parameterType);
		return new MethodParameter(method, 0);
	}

	private static MethodParameter getParameter(String methodName) {

		for (Method method : Controller.class.getMethods()) {

			if (method.getName().equals(methodName)) {
				return new MethodParameter(method, 0);
			}
		}

		throw new NoSuchMethodError(methodName);
	}

	@ProjectedPayload
	interface AnnotatedInterface {}

	interface UnannotatedInterface {}

	interface Controller {

		void with(AnnotatedInterface param);

		void withAnnotatedInterface(@ModelAttribute AnnotatedInterface param);

		void with(UnannotatedInterface param);

		void with(SampleInterface param);

		void with(List<Object> param);

		void withForeignAnnotation(@Autowired SampleInterface param);

		void withModelAttribute(@ModelAttribute SampleInterface param);

		void withModelAttributeMultipart(@ModelAttribute MultipartFile file);

		void withProjectedPayload(@ProjectedPayload SampleInterface param);

		void withProjectedPayloadMultipart(@ProjectedPayload MultipartFile file);
	}

}
