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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.web.querydsl.ReactiveQuerydslPredicateArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * Querydsl-specific web configuration for Spring Data. Registers a
 * {@link org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver} that builds up
 * {@link com.querydsl.core.types.Predicate}s from web requests.
 *
 * @author Matías Hermosilla
 * @author Mark Paluch
 * @since 1.11
 */
@Configuration(proxyBeanMethods = false)
public class ReactiveQuerydslWebConfiguration implements WebFluxConfigurer {

	@Autowired @Qualifier("webFluxConversionService") ObjectProvider<ConversionService> conversionService;
	@Autowired ObjectProvider<EntityPathResolver> resolver;
	@Autowired BeanFactory beanFactory;

	/**
	 * Default {@link ReactiveQuerydslPredicateArgumentResolver} to create Querydsl {@link Predicate} instances for Spring
	 * WebFlux controller methods.
	 *
	 * @return
	 */
	@Lazy
	@Bean
	public ReactiveQuerydslPredicateArgumentResolver querydslPredicateArgumentResolver() {
		return new ReactiveQuerydslPredicateArgumentResolver(
				beanFactory.getBean("querydslBindingsFactory", QuerydslBindingsFactory.class),
				conversionService.getIfUnique(DefaultConversionService::getSharedInstance));
	}

	@Lazy
	@Bean
	public QuerydslBindingsFactory querydslBindingsFactory() {
		return new QuerydslBindingsFactory(resolver.getIfUnique(() -> SimpleEntityPathResolver.INSTANCE));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.config.WebFluxConfigurer#configureArgumentResolvers(org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer)
	 */
	@Override
	public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
		configurer.addCustomResolver(
				beanFactory.getBean("querydslPredicateArgumentResolver", ReactiveQuerydslPredicateArgumentResolver.class));
	}

}
