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

import javax.servlet.http.HttpServletRequest;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Helper methods for web integration testing.
 *
 * @author Oliver Gierke
 */
public class WebTestUtils {

	/**
	 * Initializes web tests. Will register a {@link MockHttpServletRequest} for the current thread.
	 */
	public static void initWebTest() {
		initWebTest(new MockHttpServletRequest());
	}

	/**
	 * Initializes web tests for the given {@link HttpServletRequest} which will registered for the current thread.
	 */
	public static void initWebTest(HttpServletRequest request) {

		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
	}

	/**
	 * Creates a {@link WebApplicationContext} from the given configuration classes.
	 *
	 * @param configClasses
	 * @return
	 */
	public static WebApplicationContext createApplicationContext(Class<?>... configClasses) {
		return createApplicationContext(null, configClasses);
	}

	/**
	 * Creates a {@link WebApplicationContext} from the given configuration classes.
	 *
	 * @param classLoader gets set as ClassLoader in the context
	 * @param configClasses
	 * @return
	 */
	public static WebApplicationContext createApplicationContext(ClassLoader classLoader, Class<?>... configClasses) {

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		if (classLoader != null) {
			context.setClassLoader(classLoader);
		}

		context.setServletContext(new MockServletContext());

		for (Class<?> configClass : configClasses) {
			context.register(configClass);
		}

		context.refresh();

		return context;
	}
}
