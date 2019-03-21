/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.WebTestUtils;
import org.springframework.data.web.config.EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Integration tests for {@link EnableSpringDataWebSupport}.
 * 
 * @author Oliver Gierke
 * @see DATACMNS-330
 */
public class EnableSpringDataWebSupportIntegrationTests {

	private static final String HATEOAS = "HATEOAS_PRESENT";
	private static final String JACKSON = "JACKSON_PRESENT";

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	static class SampleConfig {

	}

	@After
	public void tearDown() {
		reEnable(HATEOAS);
		reEnable(JACKSON);
	}

	@Test
	public void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pageableResolver", "sortResolver"));

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}

	@Test
	public void registersHateoasSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver"));
		assertResolversRegistered(context, PagedResourcesAssemblerArgumentResolver.class);
	}

	@Test
	public void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		hide(HATEOAS);

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pageableResolver", "sortResolver"));
		assertThat(names, not(hasItems("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver")));
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void registersJacksonSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItem("jacksonGeoModule"));
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void doesNotRegisterJacksonSpecificComponentsIfJacksonNotPresent() throws Exception {

		hide(JACKSON);

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, not(hasItem("jacksonGeoModule")));
	}

	@SuppressWarnings("unchecked")
	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) {

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter, is(notNullValue()));
		List<HandlerMethodArgumentResolver> resolvers = adapter.getCustomArgumentResolvers();

		List<Matcher<Object>> resolverMatchers = new ArrayList<Matcher<Object>>(resolverTypes.length);

		for (Class<?> resolverType : resolverTypes) {
			resolverMatchers.add(instanceOf(resolverType));
		}

		assertThat(resolvers, hasItems(resolverMatchers.toArray(new Matcher[resolverMatchers.size()])));
	}

	private static void hide(String module) throws Exception {

		Field field = ReflectionUtils.findField(SpringDataWebConfigurationImportSelector.class, module);
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, null, false);
	}

	private static void reEnable(String module) {

		Field field = ReflectionUtils.findField(SpringDataWebConfigurationImportSelector.class, module);
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, null, true);
	}
}
