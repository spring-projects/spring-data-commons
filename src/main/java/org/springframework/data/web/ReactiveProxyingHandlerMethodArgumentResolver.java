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
package org.springframework.data.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.annotation.ModelAttributeMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@link HandlerMethodArgumentResolver} to create Proxy instances for interface based controller method parameters.
 *
 * @author Oliver Gierke
 * @author Matias Hermosilla
 * @since 3.0
 */
public class ReactiveProxyingHandlerMethodArgumentResolver extends ModelAttributeMethodArgumentResolver
		implements BeanFactoryAware, BeanClassLoaderAware {

	private static final List<String> IGNORED_PACKAGES = Arrays.asList("java", "org.springframework");

	private final SpelAwareProxyProjectionFactory proxyFactory;
	private final ObjectFactory<ConversionService> conversionService;

	/**
	 * Creates a new {@link PageableHandlerMethodArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public ReactiveProxyingHandlerMethodArgumentResolver(ObjectFactory<ConversionService> conversionService,
			ReactiveAdapterRegistry adapterRegistry, boolean annotationNotRequired) {

		super(adapterRegistry, annotationNotRequired);

		this.proxyFactory = new SpelAwareProxyProjectionFactory();
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.proxyFactory.setBeanFactory(beanFactory);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.proxyFactory.setBeanClassLoader(classLoader);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		if (!super.supportsParameter(parameter)) {
			return false;
		}

		Class<?> type = parameter.getParameterType();

		if (!type.isInterface()) {
			return false;
		}

		// Annotated parameter
		if (parameter.getParameterAnnotation(ProjectedPayload.class) != null) {
			return true;
		}

		// Annotated type
		if (AnnotatedElementUtils.findMergedAnnotation(type, ProjectedPayload.class) != null) {
			return true;
		}

		// Fallback for only user defined interfaces
		String packageName = ClassUtils.getPackageName(type);

		return !IGNORED_PACKAGES.stream().anyMatch(it -> packageName.startsWith(it));
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		MapDataBinder binder = new MapDataBinder(parameter.getParameterType(), conversionService.getObject());
		binder.bind(new MutablePropertyValues(exchange.getAttributes()));

		return Mono.just(proxyFactory.createProjection(parameter.getParameterType(), binder.getTarget()));
	}

	@Override
	protected Mono<Void> bindRequestParameters(WebExchangeDataBinder binder, ServerWebExchange request) {
		return Mono.never();
	}

}
