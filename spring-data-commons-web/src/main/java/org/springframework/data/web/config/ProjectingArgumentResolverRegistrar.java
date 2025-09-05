/*
 * Copyright 2017-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.ProxyingHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * Configuration class to register a {@link BeanPostProcessor} to augment {@link RequestMappingHandlerAdapter} with a
 * {@link ProxyingHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @soundtrack Apparat With Soap & Skin - Goodbye (Dark Theme Song - https://www.youtube.com/watch?v=66VnOdk6oto)
 */
@Configuration(proxyBeanMethods = false)
public class ProjectingArgumentResolverRegistrar {

	/**
	 * Registers a {@link BeanPostProcessor} to modify {@link RequestMappingHandlerAdapter} beans in the application
	 * context to get a {@link ProxyingHandlerMethodArgumentResolver} configured as first
	 * {@link HandlerMethodArgumentResolver}.
	 *
	 * @param conversionService the Spring MVC {@link ConversionService} in a lazy fashion, so that its initialization is
	 *          not triggered yet.
	 * @return
	 */
	@Bean
	static ProjectingArgumentResolverBeanPostProcessor projectingArgumentResolverBeanPostProcessor(
			@Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {
		return new ProjectingArgumentResolverBeanPostProcessor(conversionService);
	}

	/**
	 * A {@link BeanPostProcessor} to modify {@link RequestMappingHandlerAdapter} beans in the application context to get
	 * a {@link ProxyingHandlerMethodArgumentResolver} configured as first {@link HandlerMethodArgumentResolver}.
	 *
	 * @author Oliver Gierke
	 * @soundtrack Apparat With Soap & Skin - Goodbye (Dark Theme Song - https://www.youtube.com/watch?v=66VnOdk6oto)
	 */
	static class ProjectingArgumentResolverBeanPostProcessor
			implements BeanPostProcessor, BeanFactoryAware, BeanClassLoaderAware {

		private ProxyingHandlerMethodArgumentResolver resolver;

		/**
		 * A {@link BeanPostProcessor} to modify {@link RequestMappingHandlerAdapter} beans in the application context to
		 * get a {@link ProxyingHandlerMethodArgumentResolver} configured as first {@link HandlerMethodArgumentResolver}.
		 *
		 * @param conversionService the Spring MVC {@link ConversionService} in a lazy fashion, so that its initialization
		 *          is not triggered yet.
		 */
		ProjectingArgumentResolverBeanPostProcessor(ObjectFactory<ConversionService> conversionService) {
			this.resolver = new ProxyingHandlerMethodArgumentResolver(conversionService, false);
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.resolver.setBeanFactory(beanFactory);
		}

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.resolver.setBeanClassLoader(classLoader);
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

			if (!(bean instanceof RequestMappingHandlerAdapter)) {
				return bean;
			}

			RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
			List<HandlerMethodArgumentResolver> currentResolvers = adapter.getArgumentResolvers();

			if (currentResolvers == null) {
				throw new IllegalStateException(
						String.format("No HandlerMethodArgumentResolvers found in RequestMappingHandlerAdapter %s", beanName));
			}

			List<HandlerMethodArgumentResolver> newResolvers = new ArrayList<>(
					currentResolvers.size() + 1);
			newResolvers.add(resolver);
			newResolvers.addAll(currentResolvers);

			adapter.setArgumentResolvers(newResolvers);

			return adapter;
		}
	}
}
