/*
 * Copyright 2013-2021 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * General test utilities.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class TestUtils {

	static NativeWebRequest getWebRequest() {
		return new ServletWebRequest(new MockHttpServletRequest());
	}

	static MockServerHttpRequest getWebfluxRequest() {
		return MockServerHttpRequest.get("foo").build();
	}

	static MethodParameter getParameterOfMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

		Method method = getMethod(controller, name, argumentTypes);
		return new MethodParameter(method, 0);
	}

	static Method getMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

		try {
			return controller.getMethod(name, argumentTypes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
