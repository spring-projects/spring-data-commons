/*
 * Copyright 2013-2015 the original author or authors.
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.WebTestUtils;
import org.springframework.data.web.config.EnableSpringDataWebSupport.SpringDataWebConfigurationImportSelector;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.util.UriComponentsBuilder;

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

		public @Bean SampleController controller() {
			return new SampleController();
		}
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

		assertThat(names).contains("pageableResolver", "sortResolver");

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}

	@Test
	public void registersHateoasSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
		assertResolversRegistered(context, PagedResourcesAssemblerArgumentResolver.class);
	}

	@Test
	public void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		hide(HATEOAS);

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");
		assertThat(names).doesNotContain("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void registersJacksonSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("jacksonGeoModule");
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void doesNotRegisterJacksonSpecificComponentsIfJacksonNotPresent() throws Exception {

		hide(JACKSON);

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).doesNotContain("jacksonGeoModule");
	}

	/**
	 * @see DATACMNS-626
	 */
	@Test
	public void registersFormatters() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		ConversionService conversionService = context.getBean(ConversionService.class);

		assertThat(conversionService.canConvert(String.class, Distance.class)).isTrue();
		assertThat(conversionService.canConvert(Distance.class, String.class)).isTrue();
		assertThat(conversionService.canConvert(String.class, Point.class)).isTrue();
		assertThat(conversionService.canConvert(Point.class, String.class)).isTrue();
	}

	/**
	 * @see DATACMNS-630
	 */
	@Test
	public void createsProxyForInterfaceBasedControllerMethodParameter() throws Exception {

		WebApplicationContext applicationContext = WebTestUtils.createApplicationContext(SampleConfig.class);
		MockMvc mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("/proxy");
		builder.queryParam("name", "Foo");
		builder.queryParam("shippingAddresses[0].zipCode", "ZIP");
		builder.queryParam("shippingAddresses[0].city", "City");
		builder.queryParam("billingAddress.zipCode", "ZIP");
		builder.queryParam("billingAddress.city", "City");
		builder.queryParam("date", "2014-01-11");

		mvc.perform(post(builder.build().toString())).//
				andExpect(status().isOk());
	}

	/**
	 * @see DATACMNS-660
	 */
	@Test
	public void picksUpWebConfigurationMixins() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("sampleBean");
	}

	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) {

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		List<HandlerMethodArgumentResolver> resolvers = adapter.getCustomArgumentResolvers();

		Arrays.asList(resolverTypes).forEach(type -> assertThat(resolvers).hasAtLeastOneElementOfType(type));
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
