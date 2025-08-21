/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.web.ProjectingJacksonHttpMessageConverter;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.util.ReflectionUtils;

import org.xmlbeam.XBProjector;

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

		HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);

		createConfigWithClassLoader(
				HidingClassLoader.hide(ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper.class),
				it -> it.configureMessageConverters(builder));

		verify(builder).customMessageConverter(any(XmlBeamHttpMessageConverter.class));
		verifyNoMoreInteractions(builder);
	}

	@Test // DATACMNS-987
	void shouldNotLoadJacksonConverterWhenJaywayNotPresent() {

		HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);

		createConfigWithClassLoader(HidingClassLoader.hide(DocumentContext.class),
				it -> it.configureMessageConverters(builder));

		verify(builder).customMessageConverter(any(XmlBeamHttpMessageConverter.class));
		verifyNoMoreInteractions(builder);
	}

	@Test // DATACMNS-987
	void shouldNotLoadXBeamConverterWhenXBeamNotPresent() throws Exception {

		HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);

		ClassLoader classLoader = HidingClassLoader.hide(XBProjector.class);
		createConfigWithClassLoader(classLoader, it -> it.configureMessageConverters(builder));

		verify(builder, never()).customMessageConverter(any(XmlBeamHttpMessageConverter.class));
	}

	@Test // DATACMNS-987
	void shouldLoadAllConvertersWhenDependenciesArePresent() throws Exception {

		HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);

		createConfigWithClassLoader(getClass().getClassLoader(), it -> it.configureMessageConverters(builder));

		verify(builder).customMessageConverter(any(XmlBeamHttpMessageConverter.class));
		verify(builder).customMessageConverter(any(ProjectingJacksonHttpMessageConverter.class));
	}

	@Test // DATACMNS-1152
	void usesCustomObjectMapper() {

		createConfigWithClassLoader(getClass().getClassLoader(), it -> {

			HttpMessageConverters.ServerBuilder builder = mock(HttpMessageConverters.ServerBuilder.class);
			ArgumentCaptor<HttpMessageConverter> captor = ArgumentCaptor.forClass(HttpMessageConverter.class);

			it.configureMessageConverters(builder);
			verify(builder, atLeast(1)).customMessageConverter(captor.capture());

			// Converters contains ProjectingJackson2HttpMessageConverter with custom ObjectMapper

			assertThat(captor.getAllValues()).anySatisfy(converter -> {
				assertThat(converter).isInstanceOfSatisfying(ProjectingJacksonHttpMessageConverter.class, __ -> {
					assertThat(__.getMapper()).isSameAs(SomeConfiguration.MAPPER);
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

		try (var context = new AnnotationConfigApplicationContext(
				configClasses.toArray(new Class<?>[configClasses.size()]))) {

			context.setClassLoader(classLoader);
			callback.accept(context.getBean(SpringDataWebConfiguration.class));
		}
	}

	private static Class<?> loadWithout(Class<?> configurationClass, Class<?>... typesOfPackagesToExclude)
			throws ClassNotFoundException {

		var hidingClassLoader = HidingClassLoader.hide(typesOfPackagesToExclude);
		var loader = new ShadowingClassLoader(hidingClassLoader);
		loader.excludeClass(configurationClass.getName());

		return loader.loadClass(configurationClass.getName());
	}

	@Configuration
	static class SomeConfiguration {

		static JsonMapper MAPPER = new JsonMapper();

		@Bean
		JsonMapper mapper() {
			return MAPPER;
		}
	}
}
