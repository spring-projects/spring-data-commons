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

import example.SampleInterface;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unit tests for {@link ProxyingHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 * @author Chris Bono
 * @soundtrack Karlijn Langendijk & Sönke Meinen - Englishman In New York (Sting,
 *             https://www.youtube.com/watch?v=O7LZsqrnaaA)
 */
class ProxyingHandlerMethodArgumentResolverUnitTests {

	ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(
			() -> new DefaultConversionService(), true);

	@Test // DATACMNS-776
	void supportAnnotatedInterface() throws Exception {

		var parameter = getParameter("with", AnnotatedInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void supportsUnannotatedInterfaceFromUserPackage() throws Exception {

		var parameter = getParameter("with", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // DATACMNS-776
	void doesNotSupportUnannotatedInterfaceFromSpringNamespace() throws Exception {

		var parameter = getParameter("with", UnannotatedInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // DATACMNS-776
	void doesNotSupportCoreJavaType() throws Exception {

		var parameter = getParameter("with", List.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-2937
	void doesNotSupportForeignSpringAnnotations() throws Exception {

		var parameter = getParameter("withForeignAnnotation", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	@Test // GH-2937
	void doesSupportAtModelAttribute() throws Exception {

		var parameter = getParameter("withModelAttribute", SampleInterface.class);

		assertThat(resolver.supportsParameter(parameter)).isTrue();
	}

	@Test // GH-3258
	void doesNotSupportAtModelAttributeForMultipartParam() throws Exception {

		var parameter = getParameter("withModelAttributeMultipart", MultipartFile.class);

		assertThat(resolver.supportsParameter(parameter)).isFalse();
	}

	private static MethodParameter getParameter(String methodName, Class<?> parameterType) {

		var method = ReflectionUtils.findMethod(Controller.class, methodName, parameterType);
		return new MethodParameter(method, 0);
	}

	@ProjectedPayload
	interface AnnotatedInterface {}

	interface UnannotatedInterface {}

	interface Controller {

		void with(AnnotatedInterface param);

		void with(UnannotatedInterface param);

		void with(SampleInterface param);

		void with(List<Object> param);

		void withForeignAnnotation(@Autowired SampleInterface param);

		void withModelAttribute(@ModelAttribute SampleInterface param);

		void withModelAttributeMultipart(@ModelAttribute MultipartFile file);
	}
}
