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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * JavaConfig class to register {@link PagedResourcesAssembler} and {@link PagedResourcesAssemblerArgumentResolver}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
@Configuration
class HateoasAwareSpringDataWebConfiguration extends WebMvcConfigurationSupport {

	@Autowired
	SpringDataWebConfiguration config;

	@Bean
	public PagedResourcesAssembler<?> pagedResourcesAssembler() {
		return new PagedResourcesAssembler<Object>(config.pageableResolver(), null);
	}

	@Bean
	public PagedResourcesAssemblerArgumentResolver pagedResourcesAssemblerArgumentResolver() {
		return new PagedResourcesAssemblerArgumentResolver(config.pageableResolver(), null);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addArgumentResolvers(java.util.List)
	 */
	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		config.addArgumentResolvers(argumentResolvers);
		argumentResolvers.add(pagedResourcesAssemblerArgumentResolver());
	}
}
