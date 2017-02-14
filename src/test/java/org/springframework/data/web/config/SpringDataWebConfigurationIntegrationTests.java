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

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.classloadersupport.ClassLoaderConfiguration;
import org.springframework.data.classloadersupport.ClassLoaderRule;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverter;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Integration test for {@link SpringDataWebConfiguration}.
 *
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Oliver Gierke
 */
public class SpringDataWebConfigurationIntegrationTests {

	@Rule public ClassLoaderRule classLoader = new ClassLoaderRule();

	@Test // DATACMNS-987
	@ClassLoaderConfiguration(hidePackage = com.fasterxml.jackson.databind.ObjectMapper.class)
	public void shouldNotLoadJacksonConverterWhenJacksonNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader().extendMessageConverters(converters);

		assertThat(converters, not(hasItem(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	@ClassLoaderConfiguration(hidePackage = com.jayway.jsonpath.DocumentContext.class)
	public void shouldNotLoadJacksonConverterWhenJaywayNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader().extendMessageConverters(converters);

		assertThat(converters, not(hasItem(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	@ClassLoaderConfiguration(hidePackage = org.xmlbeam.ProjectionFactory.class)
	public void shouldNotLoadXBeamConverterWhenXBeamNotPresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader().extendMessageConverters(converters);

		assertThat(converters, not(hasItem(instanceWithClassName(XmlBeamHttpMessageConverter.class))));
	}

	@Test // DATACMNS-987
	public void shouldLoadAllConvertersWhenDependenciesArePresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader().extendMessageConverters(converters);

		assertThat(converters, hasItem(instanceWithClassName(XmlBeamHttpMessageConverter.class)));
		assertThat(converters, hasItem(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class)));
	}

	private SpringDataWebConfiguration createConfigWithClassLoader() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SpringDataWebConfiguration.class);
		context.setClassLoader(classLoader.classLoader);

		try {
			return context.getBean(SpringDataWebConfiguration.class);
		} finally {
			context.close();
		}
	}

	/**
	 * creates a Matcher that check if an object is an instance of a class with the same name as the provided class. This
	 * is necessary since we are dealing with multiple classloaders which would make a simple instanceof fail all the time
	 *
	 * @param expectedClass the class that is expected (possibly loaded by a different classloader).
	 * @return a Matcher
	 */
	private static <T> Matcher<? super T> instanceWithClassName(Class<T> expectedClass) {
		return hasProperty("class", hasProperty("name", equalTo(expectedClass.getName())));
	}
}
