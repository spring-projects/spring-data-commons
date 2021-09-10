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

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.geo.format.DistanceFormatter;
import org.springframework.data.geo.format.PointFormatter;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ProjectingJackson2HttpMessageConverter;
import org.springframework.data.web.ProxyingHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.data.web.XmlBeamHttpMessageConverter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration class to register {@link PageableHandlerMethodArgumentResolver},
 * {@link SortHandlerMethodArgumentResolver} and {@link DomainClassConverter}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Vedran Pavic
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Greg Turnquist
 */
@Configuration(proxyBeanMethods = false)
public class SpringDataWebConfiguration implements WebMvcConfigurer, BeanClassLoaderAware {

	private final ApplicationContext context;
	private final ObjectFactory<ConversionService> conversionService;
	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final Lazy<SortHandlerMethodArgumentResolver> sortResolver;
	private final Lazy<PageableHandlerMethodArgumentResolver> pageableResolver;
	private final Lazy<PageableHandlerMethodArgumentResolverCustomizer> pageableResolverCustomizer;
	private final Lazy<SortHandlerMethodArgumentResolverCustomizer> sortResolverCustomizer;

	public SpringDataWebConfiguration(ApplicationContext context,
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {

		Assert.notNull(context, "ApplicationContext must not be null!");
		Assert.notNull(conversionService, "ConversionService must not be null!");

		this.context = context;

		this.conversionService = conversionService;
		this.sortResolver = Lazy.of(() -> context.getBean("sortResolver", SortHandlerMethodArgumentResolver.class));
		this.pageableResolver = Lazy.of( //
				() -> context.getBean("pageableResolver", PageableHandlerMethodArgumentResolver.class));
		this.pageableResolverCustomizer = Lazy.of( //
				() -> context.getBeanProvider(PageableHandlerMethodArgumentResolverCustomizer.class).getIfAvailable());
		this.sortResolverCustomizer = Lazy.of( //
				() -> context.getBeanProvider(SortHandlerMethodArgumentResolverCustomizer.class).getIfAvailable());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#pageableResolver()
	 */
	@Bean
	public PageableHandlerMethodArgumentResolver pageableResolver() {

		PageableHandlerMethodArgumentResolver pageableResolver = //
				new PageableHandlerMethodArgumentResolver(sortResolver.get());
		customizePageableResolver(pageableResolver);
		return pageableResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.web.config.SpringDataWebConfiguration#sortResolver()
	 */
	@Bean
	public SortHandlerMethodArgumentResolver sortResolver() {

		SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
		customizeSortResolver(sortResolver);
		return sortResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addFormatters(org.springframework.format.FormatterRegistry)
	 */
	@Override
	public void addFormatters(FormatterRegistry registry) {

		registry.addFormatter(DistanceFormatter.INSTANCE);
		registry.addFormatter(PointFormatter.INSTANCE);

		if (!(registry instanceof FormattingConversionService)) {
			return;
		}

		FormattingConversionService conversionService = (FormattingConversionService) registry;

		DomainClassConverter<FormattingConversionService> converter = new DomainClassConverter<>(conversionService);
		converter.setApplicationContext(context);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#addArgumentResolvers(java.util.List)
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

		argumentResolvers.add(sortResolver.get());
		argumentResolvers.add(pageableResolver.get());

		ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(conversionService, true);
		resolver.setBeanFactory(context);
		forwardBeanClassLoader(resolver);

		argumentResolvers.add(resolver);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#extendMessageConverters(java.util.List)
	 */
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

		if (ClassUtils.isPresent("com.jayway.jsonpath.DocumentContext", context.getClassLoader())
				&& ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", context.getClassLoader())) {

			ObjectMapper mapper = context.getBeanProvider(ObjectMapper.class).getIfUnique(ObjectMapper::new);

			ProjectingJackson2HttpMessageConverter converter = new ProjectingJackson2HttpMessageConverter(mapper);
			converter.setBeanFactory(context);
			forwardBeanClassLoader(converter);

			converters.add(0, converter);
		}

		if (ClassUtils.isPresent("org.xmlbeam.XBProjector", context.getClassLoader())) {

			converters.add(0, context.getBeanProvider(XmlBeamHttpMessageConverter.class) //
					.getIfAvailable(XmlBeamHttpMessageConverter::new));
		}
	}

	protected void customizePageableResolver(PageableHandlerMethodArgumentResolver pageableResolver) {
		pageableResolverCustomizer.getOptional().ifPresent(c -> c.customize(pageableResolver));
	}

	protected void customizeSortResolver(SortHandlerMethodArgumentResolver sortResolver) {
		sortResolverCustomizer.getOptional().ifPresent(c -> c.customize(sortResolver));
	}

	private void forwardBeanClassLoader(BeanClassLoaderAware target) {

		if (beanClassLoader != null) {
			target.setBeanClassLoader(beanClassLoader);
		}
	}
}
