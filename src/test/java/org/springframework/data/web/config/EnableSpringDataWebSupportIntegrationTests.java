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
package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.ProxyingHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.WebTestUtils;
import org.springframework.hateoas.Link;
import org.springframework.test.util.ReflectionTestUtils;
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
 * @author Vedran Pavic
 */
class EnableSpringDataWebSupportIntegrationTests {

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	static class SampleConfig {

		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	static class PageableResolverCustomizerConfig extends SampleConfig {

		@Bean
		PageableHandlerMethodArgumentResolverCustomizer testPageableResolverCustomizer() {
			return pageableResolver -> pageableResolver.setMaxPageSize(100);
		}
	}

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	static class SortResolverCustomizerConfig extends SampleConfig {

		@Bean
		SortHandlerMethodArgumentResolverCustomizer testSortResolverCustomizer() {
			return sortResolver -> sortResolver.setSortParameter("foo");
		}
	}

	@Configuration
	@EnableSpringDataWebSupport
	static class CustomEntityPathResolver {

		static SimpleEntityPathResolver resolver = new SimpleEntityPathResolver("suffix");

		@Bean
		SimpleEntityPathResolver entityPathResolver() {
			return resolver;
		}
	}

	@Test // DATACMNS-330
	void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}

	@Test // DATACMNS-330
	void registersHateoasSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
		assertResolversRegistered(context, PagedResourcesAssemblerArgumentResolver.class);
	}

	@Test // DATACMNS-330
	void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		HidingClassLoader classLoader = HidingClassLoader.hide(Link.class);

		ApplicationContext context = WebTestUtils.createApplicationContext(classLoader, SampleConfig.class);

		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");
		assertThat(names).doesNotContain("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
	}

	@Test // DATACMNS-475
	void registersJacksonSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("jacksonGeoModule");
	}

	@Test // DATACMNS-475
	void doesNotRegisterJacksonSpecificComponentsIfJacksonNotPresent() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(HidingClassLoader.hide(ObjectMapper.class),
				SampleConfig.class);

		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).doesNotContain("jacksonGeoModule");
	}

	@Test // DATACMNS-626
	void registersFormatters() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		ConversionService conversionService = context.getBean(ConversionService.class);

		assertThat(conversionService.canConvert(String.class, Distance.class)).isTrue();
		assertThat(conversionService.canConvert(Distance.class, String.class)).isTrue();
		assertThat(conversionService.canConvert(String.class, Point.class)).isTrue();
		assertThat(conversionService.canConvert(Point.class, String.class)).isTrue();
	}

	@Test // DATACMNS-630
	void createsProxyForInterfaceBasedControllerMethodParameter() throws Exception {

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
	void picksUpWebConfigurationMixins() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("sampleBean");
	}

	@Test // DATACMNS-822
	void picksUpPageableResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(PageableResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());
		PageableHandlerMethodArgumentResolver resolver = context.getBean("pageableResolver", PageableHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testPageableResolverCustomizer");
		assertThat((Integer) ReflectionTestUtils.getField(resolver, "maxPageSize")).isEqualTo(100);
	}

	@Test // DATACMNS-822
	void picksUpSortResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SortResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());
		SortHandlerMethodArgumentResolver resolver = context.getBean("sortResolver", SortHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testSortResolverCustomizer");
		assertThat((String) ReflectionTestUtils.getField(resolver, "sortParameter")).isEqualTo("foo");
	}

	@Test // DATACMNS-1237
	void configuresProxyingHandlerMethodArgumentResolver() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);

		assertThat(adapter.getArgumentResolvers().get(0)).isInstanceOf(ProxyingHandlerMethodArgumentResolver.class);
	}

	@Test // DATACMNS-1235
	void picksUpEntityPathResolverIfRegistered() {

		WebApplicationContext context = WebTestUtils.createApplicationContext(CustomEntityPathResolver.class);

		assertThat(context.getBean(EntityPathResolver.class)).isEqualTo(CustomEntityPathResolver.resolver);
		assertThat(context.getBean(QuerydslBindingsFactory.class).getEntityPathResolver())
				.isEqualTo(CustomEntityPathResolver.resolver);
	}

	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) {

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		List<HandlerMethodArgumentResolver> resolvers = adapter.getCustomArgumentResolvers();

		Arrays.asList(resolverTypes).forEach(type -> assertThat(resolvers).hasAtLeastOneElementOfType(type));
	}
}
