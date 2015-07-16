/*
 * Copyright 2015 the original author or authors.
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringDataWebConfigurationUnitTests {

	@Mock GenericWebApplicationContext context;
	@Mock ObjectFactory<ConversionService> conversionServiceObjectFactory;

	SpringDataWebConfiguration config;

	@Before
	public void setUp() {

		config = new SpringDataWebConfiguration();
		ReflectionTestUtils.setField(config, "context", context);
		ReflectionTestUtils.setField(config, "conversionService", conversionServiceObjectFactory);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void registersQuerydslPredicateArgumentResolverAsBeanDefinitionWhenNotPresent() {

		config.addArgumentResolvers(new ArrayList<HandlerMethodArgumentResolver>());

		verify(context, times(1))
				.registerBeanDefinition(eq("querydslPredicateArgumentResolver"), any(BeanDefinition.class));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void usesExistingQuerydslPredicateArgumentResolverWhenBeanIsPresent() {

		QuerydslPredicateArgumentResolver argumentResolver = new QuerydslPredicateArgumentResolver(
				new DefaultConversionService());
		when(context.containsBean("querydslPredicateArgumentResolver")).thenReturn(true);
		when(context.getBean("querydslPredicateArgumentResolver")).thenReturn(argumentResolver);

		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<HandlerMethodArgumentResolver>();
		config.addArgumentResolvers(argumentResolvers);

		verify(context, never()).registerBeanDefinition(eq("querydslPredicateArgumentResolver"), any(BeanDefinition.class));
		assertThat(argumentResolvers, IsCollectionContaining.hasItem(argumentResolver));
	}
}
