/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.webflux;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Helper methods for web integration testing.
 *
 * @author Oliver Gierke
 */
public class WebTestUtils {

	/**
	 * Creates a {@link org.springframework.web.context.WebApplicationContext} from the given configuration classes.
	 *
	 * @param configClasses
	 * @return
	 */
	public static ApplicationContext createApplicationContext(Class<?>... configClasses) {
		return createApplicationContext(null, configClasses);
	}

	/**
	 * Creates a {@link org.springframework.web.context.WebApplicationContext} from the given configuration classes.
	 *
	 * @param classLoader gets set as ClassLoader in the context
	 * @param configClasses
	 * @return
	 */
	public static ApplicationContext createApplicationContext(ClassLoader classLoader, Class<?>... configClasses) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		if (classLoader != null) {
			context.setClassLoader(classLoader);
		}

		for (Class<?> configClass : configClasses) {
			context.register(configClass);
		}

		context.refresh();

		return context;
	}
}
