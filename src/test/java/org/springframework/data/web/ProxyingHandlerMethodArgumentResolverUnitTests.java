/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import example.SampleInterface;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;

/**
 * Unit tests for {@link ProxyingHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 * @soundtrack Karlijn Langendijk & Sönke Meinen - Englishman In New York (Sting,
 *             https://www.youtube.com/watch?v=O7LZsqrnaaA)
 */
public class ProxyingHandlerMethodArgumentResolverUnitTests {

	ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(
			new DefaultConversionService());

	@Test // DATACMNS-776
	public void supportsUnannotatedInterfaceFromUserPackage() throws Exception {

		Method method = Controller.class.getMethod("with", SampleInterface.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter), is(true));
	}

	@Test // DATACMNS-776
	public void doesNotSupportUnannotatedInterfaceFromSpringNamespace() throws Exception {

		Method method = Controller.class.getMethod("with", UnannotatedInterface.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter), is(false));
	}

	@Test // DATACMNS-776
	public void doesNotSupportCoreJavaType() throws Exception {

		Method method = Controller.class.getMethod("with", List.class);
		MethodParameter parameter = new MethodParameter(method, 0);

		assertThat(resolver.supportsParameter(parameter), is(false));
	}

	interface UnannotatedInterface {}

	interface Controller {

		void with(UnannotatedInterface param);

		void with(SampleInterface param);

		void with(List<Object> param);
	}
}
