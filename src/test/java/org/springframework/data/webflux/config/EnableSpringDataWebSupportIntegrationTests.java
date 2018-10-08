/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.webflux.config;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.classloadersupport.HidingClassLoader;
import org.springframework.data.webflux.PageableHandlerMethodArgumentResolver;
import org.springframework.data.webflux.SortHandlerMethodArgumentResolver;
import org.springframework.data.webflux.WebTestUtils;
import org.springframework.hateoas.Link;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link org.springframework.data.web.config.EnableSpringDataWebSupport}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Vedran Pavic
 */
public class EnableSpringDataWebSupportIntegrationTests {


	@Configuration
	@EnableWebFluxPagination
    @EnableWebFlux
	static class PageableResolverCustomizerConfig {

		@Bean
		public PageableHandlerMethodArgumentResolverCustomizer testPageableResolverCustomizer() {
			return pageableResolver -> pageableResolver.setMaxPageSize(100);
		}
	}

	@Configuration
	@EnableWebFluxPagination
    @EnableWebFlux
	static class SortResolverCustomizerConfig {

		@Bean
		public SortHandlerMethodArgumentResolverCustomizer testSortResolverCustomizer() {
			return sortResolver -> sortResolver.setSortParameter("foo");
		}
	}


	@Test
	public void registersBasicBeanDefinitions() throws Exception {

		ApplicationContext context = WebTestUtils.createApplicationContext(SortResolverCustomizerConfig.class, PageableResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");

		assertResolversRegistered(context, SortHandlerMethodArgumentResolver.class,
				PageableHandlerMethodArgumentResolver.class);
	}


	@Test
	public void doesNotRegisterHateoasSpecificComponentsIfHateoasNotPresent() throws Exception {

		HidingClassLoader classLoader = HidingClassLoader.hide(Link.class);

		ApplicationContext context = WebTestUtils.createApplicationContext(classLoader, SortResolverCustomizerConfig.class, PageableResolverCustomizerConfig.class);

		List<String> names = Arrays.asList(context.getBeanDefinitionNames());

		assertThat(names).contains("pageableResolver", "sortResolver");
	}



	@Test
	public void picksUpPageableResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(PageableResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());
		PageableHandlerMethodArgumentResolver resolver = context.getBean(PageableHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testPageableResolverCustomizer");
		assertThat((Integer) ReflectionTestUtils.getField(resolver, "maxPageSize")).isEqualTo(100);
	}

	@Test
	public void picksUpSortResolverCustomizer() {

		ApplicationContext context = WebTestUtils.createApplicationContext(SortResolverCustomizerConfig.class);
		List<String> names = Arrays.asList(context.getBeanDefinitionNames());
		SortHandlerMethodArgumentResolver resolver = context.getBean(SortHandlerMethodArgumentResolver.class);

		assertThat(names).contains("testSortResolverCustomizer");
		assertThat((String) ReflectionTestUtils.getField(resolver, "sortParameter")).isEqualTo("foo");
	}



	private static void assertResolversRegistered(ApplicationContext context, Class<?>... resolverTypes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
        ArgumentResolverConfigurer argumentResolverConfigurer = adapter.getArgumentResolverConfigurer();
        boolean accessible = true;
        Method getCustomResolvers = null;
        try {
            getCustomResolvers = argumentResolverConfigurer.getClass()
                    .getDeclaredMethod("getCustomResolvers");
            accessible = getCustomResolvers.isAccessible();
            if (!accessible) {
                getCustomResolvers.setAccessible(true);
            }
            @SuppressWarnings("unchecked")
            List<HandlerMethodArgumentResolver> resolvers = (List<HandlerMethodArgumentResolver>) getCustomResolvers
                    .invoke(argumentResolverConfigurer);
            Arrays.asList(resolverTypes).forEach(type -> assertThat(resolvers).hasAtLeastOneElementOfType(type));
            getCustomResolvers.setAccessible(false);
        } finally {
            if (!accessible) {
                getCustomResolvers.setAccessible(false);
            }
        }
	}
}
