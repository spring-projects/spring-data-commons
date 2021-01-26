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

import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
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
 * @author Greg Turnquist
 */
@Configuration(proxyBeanMethods = false)
public class HateoasAwareSpringDataWebConfiguration extends SpringDataWebConfiguration {

	private final Lazy<HateoasSortHandlerMethodArgumentResolver> sortResolver;
	private final Lazy<HateoasPageableHandlerMethodArgumentResolver> pageableResolver;
	private final Lazy<PagedResourcesAssemblerArgumentResolver> argumentResolver;

	/**
	 * @param context must not be {@literal null}.
	 * @param conversionService must not be {@literal null}.
	 */
	public HateoasAwareSpringDataWebConfiguration(ApplicationContext context,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {

		super(context, conversionService);

		this.sortResolver = Lazy
				.of(() -> context.getBean("sortResolver", HateoasSortHandlerMethodArgumentResolver.class));
		this.pageableResolver = Lazy
				.of(() -> context.getBean("pageableResolver", HateoasPageableHandlerMethodArgumentResolver.class));
		this.argumentResolver = Lazy.of(() -> context.getBean("pagedResourcesAssemblerArgumentResolver",
				PagedResourcesAssemblerArgumentResolver.class));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#pageableResolver()
	 */
	@Override
	@Bean
	public HateoasPageableHandlerMethodArgumentResolver pageableResolver() {

		HateoasPageableHandlerMethodArgumentResolver pageableResolver = new HateoasPageableHandlerMethodArgumentResolver(
				this.sortResolver.get());
		customizePageableResolver(pageableResolver);
		return pageableResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#sortResolver()
	 */
	@Bean
	@Override
	public HateoasSortHandlerMethodArgumentResolver sortResolver() {

		HateoasSortHandlerMethodArgumentResolver sortResolver = new HateoasSortHandlerMethodArgumentResolver();
		customizeSortResolver(sortResolver);
		return sortResolver;
	}

	@Bean
	public PagedResourcesAssembler<?> pagedResourcesAssembler() {
		return new PagedResourcesAssembler<>(pageableResolver.get(), null);
	}

	@Bean
	public PagedResourcesAssemblerArgumentResolver pagedResourcesAssemblerArgumentResolver() {
		return new PagedResourcesAssemblerArgumentResolver(pageableResolver.get());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addArgumentResolvers(java.util.List)
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

		super.addArgumentResolvers(argumentResolvers);

		argumentResolvers.add(argumentResolver.get());
	}
}
