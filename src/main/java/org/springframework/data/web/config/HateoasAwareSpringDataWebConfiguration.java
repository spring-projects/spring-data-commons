/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * JavaConfig class to register {@link PagedResourcesAssembler} and {@link PagedResourcesAssemblerArgumentResolver}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Ben Hale
 * @author Vedran Pavic
 * @author Mark Paluch
 */
@Configuration(proxyBeanMethods = false)
public class HateoasAwareSpringDataWebConfiguration extends SpringDataWebConfiguration {

	private final ApplicationContext context;

	/**
	 * @param context must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public HateoasAwareSpringDataWebConfiguration(ApplicationContext context,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {
		super(context, conversionService);
		this.context = context;
		this.sortResolver = Lazy
				.of(() -> context.getBean("hateoasSortResolver", HateoasSortHandlerMethodArgumentResolver.class));
		this.pageableResolver = Lazy
				.of(() -> context.getBean("hateoasPageableResolver", HateoasPageableHandlerMethodArgumentResolver.class));

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	public HateoasPageableHandlerMethodArgumentResolver hateoasPageableResolver() {

		HateoasPageableHandlerMethodArgumentResolver pageableResolver = new HateoasPageableHandlerMethodArgumentResolver(
				(HateoasSortHandlerMethodArgumentResolver) this.sortResolver.get());
		customizePageableResolver(pageableResolver);
		return pageableResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#sortResolver()
	 */
	@Bean
	public HateoasSortHandlerMethodArgumentResolver hateoasSortResolver() {

		HateoasSortHandlerMethodArgumentResolver sortResolver = new HateoasSortHandlerMethodArgumentResolver();
		customizeSortResolver(sortResolver);
		return sortResolver;
	}

	@Bean
	public PagedResourcesAssembler<?> pagedResourcesAssembler(
			HateoasPageableHandlerMethodArgumentResolver pageableResolver) {
		return new PagedResourcesAssembler<>(pageableResolver, null);
	}

	@Bean
	public PagedResourcesAssemblerArgumentResolver pagedResourcesAssemblerArgumentResolver(
			HateoasPageableHandlerMethodArgumentResolver pageableResolver) {
		return new PagedResourcesAssemblerArgumentResolver(pageableResolver, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addArgumentResolvers(java.util.List)
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		super.addArgumentResolvers(argumentResolvers);
		argumentResolvers
				.add(context.getBean("pagedResourcesAssemblerArgumentResolver", PagedResourcesAssemblerArgumentResolver.class));
	}
}
