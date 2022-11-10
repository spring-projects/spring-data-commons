/*
 * Copyright 2013-2022 the original author or authors.
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

import com.custom.querydslpredicatebuilder.QuerydslPredicateBuilderCustom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimplePath;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilderCustomizer;
import org.springframework.data.web.*;
import org.springframework.hateoas.Link;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	@EnableWebFlux
	@Import(ReactiveQuerydslWebConfiguration.class)
	static class WebFluxSampleConfig {

		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@Configuration
	@EnableWebFlux
	@ComponentScan(basePackageClasses = QuerydslPredicateBuilderCustom.class)
	@Import(ReactiveQuerydslWebConfiguration.class)
	static class WebFluxSampleConfigWithCustomQuerydslPredicateBuilder {

		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@Configuration
	static class QuerydslPredicateBuilderConfig {
		@Bean
		QuerydslPredicateBuilderCustomizer querydslPredicateBuilderCustomizer() {
			return (type, values, bindings) -> {
				BooleanBuilder builder = new BooleanBuilder();
				SimplePath<QUser> pathUser = Expressions.path(QUser.class, "user");
				for (var entry : values.entrySet()) {
					Path<String> path = ExpressionUtils.path(String.class, pathUser, entry.getKey());
					builder.or(Expressions.predicate(Ops.STARTS_WITH_IC, path, Expressions.constant(entry.getValue())));
				}
				return builder.getValue();
			};
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

	@Configuration
	@EnableWebMvc
	@EnableSpringDataWebSupport
	@ComponentScan(basePackageClasses = QuerydslPredicateBuilderCustom.class)
	static class SampleConfigWithCustomQuerydslPredicateBuilder {
		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@Test // DATACMNS-330
	void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}

	@Test // DATACMNS-330
	void registersHateoasSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
		assertResolversRegistered(context, PagedResourcesAssemblerArgumentResolver.class);
	}

	@Test // DATACMNS-330
	void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		var classLoader = HidingClassLoader.hide(Link.class);

		ApplicationContext context = WebTestUtils.createApplicationContext(classLoader, SampleConfig.class);

		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");
		assertThat(names).doesNotContain("pagedResourcesAssembler", "pagedResourcesAssemblerArgumentResolver");
	}

	@Test // DATACMNS-475
	void registersJacksonSpecificBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);
		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("jacksonGeoModule");
	}

	@Test // DATACMNS-475
	void doesNotRegisterJacksonSpecificComponentsIfJacksonNotPresent() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(HidingClassLoader.hide(ObjectMapper.class),
				SampleConfig.class);

		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).doesNotContain("jacksonGeoModule");
	}

	@Test // DATACMNS-626
	void registersFormatters() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		var conversionService = context.getBean(ConversionService.class);

		assertThat(conversionService.canConvert(String.class, Distance.class)).isTrue();
		assertThat(conversionService.canConvert(Distance.class, String.class)).isTrue();
		assertThat(conversionService.canConvert(String.class, Point.class)).isTrue();
		assertThat(conversionService.canConvert(Point.class, String.class)).isTrue();
	}

	@Test // DATACMNS-630
	void createsProxyForInterfaceBasedControllerMethodParameter() throws Exception {

		var applicationContext = WebTestUtils.createApplicationContext(SampleConfig.class);
		var mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

		var builder = UriComponentsBuilder.fromUriString("/proxy");
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
		var names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("sampleBean");
	}

	@Test // DATACMNS-822
	void picksUpPageableResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(PageableResolverCustomizerConfig.class);
		var names = Arrays.asList(context.getBeanDefinitionNames());
		var resolver = context.getBean("pageableResolver", PageableHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testPageableResolverCustomizer");
		assertThat((Integer) ReflectionTestUtils.getField(resolver, "maxPageSize")).isEqualTo(100);
	}

	@Test // DATACMNS-822
	void picksUpSortResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SortResolverCustomizerConfig.class);
		var names = Arrays.asList(context.getBeanDefinitionNames());
		var resolver = context.getBean("sortResolver", SortHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testSortResolverCustomizer");
		assertThat((String) ReflectionTestUtils.getField(resolver, "sortParameter")).isEqualTo("foo");
	}

	@Test // DATACMNS-1237
	void configuresProxyingHandlerMethodArgumentResolver() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SampleConfig.class);

		var adapter = context.getBean(RequestMappingHandlerAdapter.class);

		assertThat(adapter.getArgumentResolvers().get(0)).isInstanceOf(ProxyingHandlerMethodArgumentResolver.class);
	}

	@Test // DATACMNS-1235
	void picksUpEntityPathResolverIfRegistered() {

		var context = WebTestUtils.createApplicationContext(CustomEntityPathResolver.class);

		assertThat(context.getBean(EntityPathResolver.class)).isEqualTo(CustomEntityPathResolver.resolver);
		assertThat(context.getBean(QuerydslBindingsFactory.class).getEntityPathResolver())
				.isEqualTo(CustomEntityPathResolver.resolver);
	}

	@Test
	void createsTestForCustomPredicate() throws Exception {

		var applicationContext = WebTestUtils.createApplicationContext(SampleConfigWithCustomQuerydslPredicateBuilder.class);
		var mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

		var builder = UriComponentsBuilder.fromUriString("/predicate");
		builder.queryParam("firstname", "Foo");
		builder.queryParam("lastname", "Bar");

		mvc.perform(post(builder.build().toString())).
				andExpect(status().isOk())
				.andExpect(content().string(QUser.user.firstname.eq("Foo").or(QUser.user.lastname.eq("Bar")).toString()));

		//Default should be and
		applicationContext = WebTestUtils.createApplicationContext(SampleConfig.class);
		mvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

		mvc.perform(post(builder.build().toString())).
				andExpect(status().isOk())
				.andExpect(content().string(QUser.user.firstname.eq("Foo").and(QUser.user.lastname.eq("Bar")).toString()));

		applicationContext = WebTestUtils.createApplicationContext(WebFluxSampleConfigWithCustomQuerydslPredicateBuilder.class);

		var client = WebTestClient.bindToApplicationContext(applicationContext).build();
		client.get().uri(URI.create("/predicateMono?firstname=Foo&lastname=Bar"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo(QUser.user.firstname.eq("Foo").or(QUser.user.lastname.eq("Bar")).toString());

		//Default query is AND operator
		applicationContext = WebTestUtils.createApplicationContext(WebFluxSampleConfig.class);
		client = WebTestClient.bindToApplicationContext(applicationContext).build();

		client.get().uri(URI.create("/predicateMono?firstname=Foo&lastname=Bar"))
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.isEqualTo(QUser.user.firstname.eq("Foo").and(QUser.user.lastname.eq("Bar")).toString());

	}

	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) {

		var adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		var resolvers = adapter.getCustomArgumentResolvers();

		Arrays.asList(resolverTypes).forEach(type -> assertThat(resolvers).hasAtLeastOneElementOfType(type));
	}
}
