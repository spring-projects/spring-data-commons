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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Configuration class to register {@link PageableHandlerMethodArgumentResolver},
 * {@link SortHandlerMethodArgumentResolver} and {@link DomainClassConverter}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 */
@Configuration
public class SpringDataWebConfiguration extends WebMvcConfigurerAdapter {

	@Autowired private ApplicationContext context;

	@Bean
	public PageableHandlerMethodArgumentResolver pageableResolver() {
		return createPageableResolver();
	}

	// Necessary to prevent IllegalStateException: Singleton 'pageableResolver' isn't currently in creation for HATEOAS
	PageableHandlerMethodArgumentResolver createPageableResolver() {
		return new PageableHandlerMethodArgumentResolver();
	}

	@Bean
	public SortHandlerMethodArgumentResolver sortResolver() {
		return createSortResolver();
	}

	// Necessary to prevent IllegalStateException: Singleton 'sortResolver' isn't currently in creation for HATEOAS
	public SortHandlerMethodArgumentResolver createSortResolver() {
		return new SortHandlerMethodArgumentResolver();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addFormatters(org.springframework.format.FormatterRegistry)
	 */
	@Override
	public void addFormatters(FormatterRegistry registry) {

		if (!(registry instanceof FormattingConversionService)) {
			return;
		}

		registerDomainClassConverterFor((FormattingConversionService) registry);
	}

	private void registerDomainClassConverterFor(FormattingConversionService conversionService) {

		DomainClassConverter<FormattingConversionService> converter = new DomainClassConverter<FormattingConversionService>(
				conversionService);
		converter.setApplicationContext(context);
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
