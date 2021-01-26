/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverter;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.util.ReflectionUtils;
import org.xmlbeam.XBProjector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;

/**
 * Integration test for {@link SpringDataWebConfiguration}.
 *
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Oliver Gierke
 */
class SpringDataWebConfigurationIntegrationTests {

	@Test // DATACMNS-987
	void shouldNotLoadJacksonConverterWhenJacksonNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(HidingClassLoader.hide(ObjectMapper.class),
				it -> it.extendMessageConverters(converters));

		assertThat(converters).areNot(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	void shouldNotLoadJacksonConverterWhenJaywayNotPresent() {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(HidingClassLoader.hide(DocumentContext.class),
				it -> it.extendMessageConverters(converters));

		assertThat(converters).areNot(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	void shouldNotLoadXBeamConverterWhenXBeamNotPresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		ClassLoader classLoader = HidingClassLoader.hide(XBProjector.class);
		createConfigWithClassLoader(classLoader, it -> it.extendMessageConverters(converters));

		assertThat(converters).areNot(instanceWithClassName(XmlBeamHttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	void shouldLoadAllConvertersWhenDependenciesArePresent() throws Exception {

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

		createConfigWithClassLoader(getClass().getClassLoader(), it -> it.extendMessageConverters(converters));

		assertThat(converters).haveAtLeastOne(instanceWithClassName(XmlBeamHttpMessageConverter.class));
		assertThat(converters).haveAtLeastOne(instanceWithClassName(ProjectingJackson2HttpMessageConverter.class));
	}

	@Test // DATACMNS-1152
	void usesCustomObjectMapper() {

		createConfigWithClassLoader(getClass().getClassLoader(), it -> {

			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			it.extendMessageConverters(converters);

			// Converters contains ProjectingJackson2HttpMessageConverter with custom ObjectMapper
			assertThat(converters).anySatisfy(converter -> {
				assertThat(converter).isInstanceOfSatisfying(ProjectingJackson2HttpMessageConverter.class, __ -> {
					assertThat(__.getObjectMapper()).isSameAs(SomeConfiguration.MAPPER);
				});
			});

		}, SomeConfiguration.class);
	}

	@Test // DATACMNS-1386
	void doesNotFailWithJacksonMissing() throws Exception {
		ReflectionUtils.getUniqueDeclaredMethods(loadWithout(SpringDataWebConfiguration.class, ObjectMapper.class));
	}

	private static void createConfigWithClassLoader(ClassLoader classLoader,
			Consumer<SpringDataWebConfiguration> callback, Class<?>... additionalConfigurationClasses) {

		List<Class<?>> configClasses = new ArrayList<>(Arrays.asList(additionalConfigurationClasses));
		configClasses.add(SpringDataWebConfiguration.class);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configClasses.toArray(new Class<?>[configClasses.size()]))) {

			context.setClassLoader(classLoader);
			callback.accept(context.getBean(SpringDataWebConfiguration.class));
		}
	}

	private static Class<?> loadWithout(Class<?> configurationClass, Class<?>... typesOfPackagesToExclude)
			throws ClassNotFoundException {

		HidingClassLoader hidingClassLoader = HidingClassLoader.hide(typesOfPackagesToExclude);
		ShadowingClassLoader loader = new ShadowingClassLoader(hidingClassLoader);
		loader.excludeClass(configurationClass.getName());

		return loader.loadClass(configurationClass.getName());
	}

	/**
	 * Creates a {@link Condition} that checks if an object is an instance of a class with the same name as the provided
	 * class. This is necessary since we are dealing with multiple classloaders which would make a simple instanceof fail
	 * all the time
	 *
	 * @param expectedClass the class that is expected (possibly loaded by a different classloader).
	 * @return a {@link Condition}
	 */
	private static Condition<Object> instanceWithClassName(Class<?> expectedClass) {

		return new Condition<>(it -> it.getClass().getName().equals(expectedClass.getName()), //
				"with class name %s!", expectedClass.getName());
	}

	@Configuration
	static class SomeConfiguration {

		static ObjectMapper MAPPER = new ObjectMapper();

		@Bean
		ObjectMapper mapper() {
			return MAPPER;
		}
	}
}
