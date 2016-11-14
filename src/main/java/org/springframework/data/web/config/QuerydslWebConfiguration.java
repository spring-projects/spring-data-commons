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

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.web.querydsl.QuerydslPredicateArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.querydsl.core.types.Predicate;

/**
 * Querydsl-specific web configuration for Spring Data. Registers a {@link HandlerMethodArgumentResolver} that builds up
 * {@link Predicate}s from web requests.
 * 
 * @author Oliver Gierke
 * @since 1.11
 * @soundtrack Anika Nilles - Alter Ego
 */
@Configuration
public class QuerydslWebConfiguration extends WebMvcConfigurerAdapter {

	@Autowired @Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService;

	/**
	 * Default {@link QuerydslPredicateArgumentResolver} to create Querydsl {@link Predicate} instances for Spring MVC
	 * controller methods.
	 * 
	 * @return
	 */
	@Lazy
	@Bean
	public QuerydslPredicateArgumentResolver querydslPredicateArgumentResolver() {
		return new QuerydslPredicateArgumentResolver(querydslBindingsFactory(), Optional.of(conversionService.getObject()));
	}

	@Lazy
	@Bean
	public QuerydslBindingsFactory querydslBindingsFactory() {
		return new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addArgumentResolvers(java.util.List)
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(0, querydslPredicateArgumentResolver());
	}
}
