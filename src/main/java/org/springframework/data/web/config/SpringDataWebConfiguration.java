/*
 * Copyright 2013 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configuration class to register {@link PageableHandlerMethodArgumentResolver},
 * {@link SortHandlerMethodArgumentResolver} and {@link DomainClassConverter}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
@Configuration
class SpringDataWebConfiguration extends WebMvcConfigurerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpringDataWebConfiguration.class);

	@Autowired(required = false)
	@Qualifier("mvcConversionService")
	FormattingConversionService conversionService;

	@Bean
	public PageableHandlerMethodArgumentResolver pageableResolver() {
		return new PageableHandlerMethodArgumentResolver();
	}

	@Bean
	public SortHandlerMethodArgumentResolver sortResolver() {
		return new SortHandlerMethodArgumentResolver();
	}

	@Bean
	public DomainClassConverter<FormattingConversionService> mvcDomainClassConverter() {

		if (conversionService == null) {

			LOGGER.warn("No default Spring MVC FormattingConversionService registered! Have you forgotten to "
					+ "use @EnableWebMvc or register a configuration class extending WebMvcConfigurationSupport?");

			return null;
		}

		return new DomainClassConverter<FormattingConversionService>(conversionService);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addArgumentResolvers(java.util.List)
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

		argumentResolvers.add(pageableResolver());
		argumentResolvers.add(sortResolver());
	}
}
