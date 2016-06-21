/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * @author Christoph Strobl
 */
public class SpringDataWebConfigurationUnitTests {

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldNotAddQuerydslPredicateArgumentResolverWhenQuerydslNotPresent() throws Exception {

		ClassLoader classLoader = initClassLoader();

		Object context = classLoader.loadClass("org.springframework.web.context.support.GenericWebApplicationContext")
				.newInstance();
		Object objectFactory = classLoader
				.loadClass("org.springframework.data.web.config.SpringDataWebConfigurationUnitTests$ObjectFactoryImpl")
				.newInstance();

		Object config = classLoader.loadClass("org.springframework.data.web.config.SpringDataWebConfiguration")
				.getDeclaredConstructors()[0].newInstance(context, objectFactory);

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

		invokeMethod(config, "addArgumentResolvers", argumentResolvers);

		for (Object resolver : argumentResolvers) {
			if (resolver instanceof QuerydslPredicateArgumentResolver) {
				fail("QuerydslPredicateArgumentResolver should not be present when Querydsl not on path");
			}
		}
	}

	private ClassLoader initClassLoader() {

		ClassLoader classLoader = new ShadowingClassLoader(URLClassLoader.getSystemClassLoader()) {

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {

				if (name.startsWith("com.mysema")) {
					throw new ClassNotFoundException();
				}

				return super.loadClass(name);
			}

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {

				if (name.startsWith("com.mysema")) {
					throw new ClassNotFoundException();
				}

				return super.findClass(name);
			}
		};

		return classLoader;
	}

	public static class ObjectFactoryImpl implements ObjectFactory<ConversionService> {

		@Override
		public ConversionService getObject() throws BeansException {
			return null;
		}
	}
}
