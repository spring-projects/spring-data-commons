/*
 * Copyright 2022-2022 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.geo.format.DistanceFormatter;
import org.springframework.data.geo.format.PointFormatter;
import org.springframework.data.repository.support.DomainClassConverter;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.*;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

/**
 * Configuration class to register {@link PageableHandlerMethodArgumentResolver},
 * {@link SortHandlerMethodArgumentResolver} and {@link DomainClassConverter}.
 *
 * @author Oliver Gierke
 * @author Vedran Pavic
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Greg Turnquist
 * @author Matías Hermosilla
 * @since 3.0
 */
public class SpringDataWebFluxConfiguration implements WebFluxConfigurer, BeanClassLoaderAware {

	private final ApplicationContext context;
	private final ObjectFactory<ConversionService> conversionService;
	private @Nullable ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final Lazy<SortHandlerMethodArgumentResolver> sortResolver;
	private final Lazy<PageableHandlerMethodArgumentResolver> pageableResolver;
	private final Lazy<PageableHandlerMethodArgumentResolverCustomizer> pageableResolverCustomizer;
	private final Lazy<SortHandlerMethodArgumentResolverCustomizer> sortResolverCustomizer;

	public SpringDataWebFluxConfiguration(ApplicationContext context,
			@Qualifier("webFluxConversionService") ObjectFactory<ConversionService> conversionService) {

		Assert.notNull(context, "ApplicationContext must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");

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

	@Override
  public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Bean
  public PageableHandlerMethodArgumentResolver pageableResolver() {

		PageableHandlerMethodArgumentResolver pageableResolver = //
				new PageableHandlerMethodArgumentResolver(sortResolver.get());
		customizePageableResolver(pageableResolver);
		return pageableResolver;
	}

	@Bean
  public SortHandlerMethodArgumentResolver sortResolver() {

		SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
		customizeSortResolver(sortResolver);
		return sortResolver;
	}

	@Override
  public void addFormatters(FormatterRegistry registry) {

		registry.addFormatter(DistanceFormatter.INSTANCE);
		registry.addFormatter(PointFormatter.INSTANCE);

		if (!(registry instanceof FormattingConversionService conversionService)) {
			return;
		}

		DomainClassConverter<FormattingConversionService> converter = new DomainClassConverter<FormattingConversionService>(
				conversionService);
		converter.setApplicationContext(context);
	}

	@Override
  public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {

		configurer.addCustomResolver((HandlerMethodArgumentResolver) sortResolver.get());
		configurer.addCustomResolver((HandlerMethodArgumentResolver) pageableResolver.get());

		ProxyingHandlerMethodArgumentResolver resolver = new ProxyingHandlerMethodArgumentResolver(conversionService, true);
		resolver.setBeanFactory(context);
		forwardBeanClassLoader(resolver);

		configurer.addCustomResolver((HandlerMethodArgumentResolver) resolver);
	}

	@Override
  public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {

		if (ClassUtils.isPresent("com.jayway.jsonpath.DocumentContext", context.getClassLoader()) && ClassUtils.isPresent(
				"com.fasterxml.jackson.databind.ObjectMapper", context.getClassLoader())) {

			ObjectMapper mapper = context.getBeanProvider(ObjectMapper.class).getIfUnique(ObjectMapper::new);

			ProjectingJackson2JsonDecoder decoder = new ProjectingJackson2JsonDecoder(mapper);
			decoder.setBeanFactory(context);
			forwardBeanClassLoader(decoder);

			configurer.customCodecs().register(decoder);

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
