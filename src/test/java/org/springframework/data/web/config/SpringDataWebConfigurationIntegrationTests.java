/*
 * Copyright 2015-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverter;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.instrument.classloading.ShadowingClassLoader;

/**
 * Integration test for {@link SpringDataWebConfiguration}.
 *
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public class SpringDataWebConfigurationIntegrationTests {

	@Test // DATACMNS-987
	public void shouldNotLoadJacksonConverterWhenJacksonNotPresent() {

		SpringDataWebConfiguration config = createConfigWithClassLoaderExcluding("com.fasterxml.jackson");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		config.extendMessageConverters(converters);

		assertThat(converters, (Matcher) not(hasItem( //
				instanceWithClassName(ProjectingJackson2HttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	public void shouldNotLoadJacksonConverterWhenJaywayNotPresent() {

		SpringDataWebConfiguration config = createConfigWithClassLoaderExcluding("com.jayway");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		config.extendMessageConverters(converters);

		assertThat(converters, (Matcher) not(hasItem( //
				instanceWithClassName(ProjectingJackson2HttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	public void shouldNotLoadXBeamConverterWhenXBeamNotPresent() throws Exception {

		SpringDataWebConfiguration config = createConfigWithClassLoaderExcluding("org.xmlbeam");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		config.extendMessageConverters(converters);

		assertThat(converters, (Matcher) not(hasItem( //
				instanceWithClassName(XmlBeamHttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	public void shouldLoadAllConvertersWhenDependenciesArePresent() throws Exception {

		SpringDataWebConfiguration config = createConfigWithClassLoaderExcluding("load.everything");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		config.extendMessageConverters(converters);

		assertThat(converters,
				containsInAnyOrder( //
						instanceWithClassName(XmlBeamHttpMessageConverter.class), //
						instanceWithClassName(ProjectingJackson2HttpMessageConverter.class)));
	}

	private SpringDataWebConfiguration createConfigWithClassLoaderExcluding(String excludedClassNamePrefix) {

		ClassLoader classLoader = initClassLoader(excludedClassNamePrefix);

		SpringDataWebConfiguration config = new SpringDataWebConfiguration();
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.setClassLoader(classLoader);

		setField(config, "context", applicationContext);

		return config;
	}

	/**
	 * creates a Matcher that check if an object is an instance of a class with the same name as the provided class. This
	 * is necessary since we are dealing with multiple classloaders which would make a simple instanceof fail all the time
	 *
	 * @param expectedClass the class that is expected (possibly loaded by a different classloader).
	 * @return a Matcher
	 */
	private Matcher<Object> instanceWithClassName(Class<?> expectedClass) {
		return hasProperty("class", hasProperty("name", equalTo(expectedClass.getName())));
	}

	private ClassLoader initClassLoader(final String excludedClassNamePrefix) {

		return new ShadowingClassLoader(URLClassLoader.getSystemClassLoader()) {

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {

				if (name.startsWith(excludedClassNamePrefix)) {
					throw new ClassNotFoundException();
				}

				return super.loadClass(name);
			}

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {

				if (name.startsWith(excludedClassNamePrefix)) {
					throw new ClassNotFoundException();
				}

				return super.findClass(name);
			}
		};
	}

	public static class ObjectFactoryImpl implements ObjectFactory<ConversionService> {

		@Override
		public ConversionService getObject() throws BeansException {
			return null;
		}
	}
}
