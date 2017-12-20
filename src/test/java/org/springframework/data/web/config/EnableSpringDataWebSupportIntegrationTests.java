/*
 * Copyright 2013-2017 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.ProxyingHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.WebTestUtils;
import org.springframework.hateoas.Link;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link EnableSpringDataWebSupport}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class EnableSpringDataWebSupportIntegrationTests {

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	static class SampleConfig {

		public @Bean SampleController controller() {
			return new SampleController();
		}
	}

	@Test // DATACMNS-330
	public void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pageableResolver", "sortResolver"));

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}

	@Test // DATACMNS-330
	public void registersHateoasSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver"));
		assertResolversRegistered(context, PagedResourcesAssemblerArgumentResolver.class);
	}

	@Test // DATACMNS-330
	// @ClassLoaderConfiguration(hidePackage = Link.class)
	public void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		HidingClassLoader classLoader = HidingClassLoader.hide(Link.class);

		ApplicationContext context = WebTestUtils.createApplicationContext(classLoader, SampleConfig.class);

		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItems("pageableResolver", "sortResolver"));
		assertThat(names, not(hasItems("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver")));
	}

	@Test // DATACMNS-475
	public void registersJacksonSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItem("jacksonGeoModule"));
	}

	@Test // DATACMNS-475
	public void doesNotRegisterJacksonSpecificComponentsIfJacksonNotPresent() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(HidingClassLoader.hide(ObjectMapper.class),
				SampleConfig.class);

		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, not(hasItem("jacksonGeoModule")));
	}

	@Test // DATACMNS-626
	public void registersFormatters() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		ConversionService conversionService = context.getBean(ConversionService.class);

		assertThat(conversionService.canConvert(String.class, Distance.class), is(true));
		assertThat(conversionService.canConvert(Distance.class, String.class), is(true));
		assertThat(conversionService.canConvert(String.class, Point.class), is(true));
		assertThat(conversionService.canConvert(Point.class, String.class), is(true));
	}

	@Test // DATACMNS-630
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

	@Test // DATACMNS-660
	public void picksUpWebConfigurationMixins() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names, hasItem("sampleBean"));
	}

	@Test // DATACMNS-1237
	public void configuresProxyingHandlerMethodArgumentResolver() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);

		assertThat(adapter.getArgumentResolvers().get(0), is(instanceOf(ProxyingHandlerMethodArgumentResolver.class)));
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
}
