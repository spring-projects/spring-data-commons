/*
 * Copyright 2015-2018 the original author or authors.
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * {@link HandlerMethodArgumentResolver} to create Proxy instances for interface based controller method parameters.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
public class ProxyingHandlerMethodArgumentResolver extends ModelAttributeMethodProcessor
		implements BeanFactoryAware, BeanClassLoaderAware {

	private static final List<String> IGNORED_PACKAGES = Arrays.asList("java", "org.springframework");

	private final SpelAwareProxyProjectionFactory proxyFactory;
	private final ObjectFactory<ConversionService> conversionService;

	/**
	 * Creates a new {@link PageableHandlerMethodArgumentResolver} using the given {@link ConversionService} and the
	 * {@link ModelAttribute} annotation not required.
	 *
	 * @param conversionService must not be {@literal null}.
	 * @deprecated use {@link #ProxyingHandlerMethodArgumentResolver(ObjectFactory, boolean)} instead.
	 */
	@Deprecated
	public ProxyingHandlerMethodArgumentResolver(final ConversionService conversionService) {
		this(() -> conversionService, true);
	}

	/**
	 * Creates a new {@link PageableHandlerMethodArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public ProxyingHandlerMethodArgumentResolver(ObjectFactory<ConversionService> conversionService,
			boolean annotationNotRequired) {

		super(annotationNotRequired);

		this.proxyFactory = new SpelAwareProxyProjectionFactory();
		this.conversionService = conversionService;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.proxyFactory.setBeanFactory(beanFactory);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.proxyFactory.setBeanClassLoader(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.annotation.ModelAttributeMethodProcessor#createAttribute(java.lang.String, org.springframework.core.MethodParameter, org.springframework.web.bind.support.WebDataBinderFactory, org.springframework.web.context.request.NativeWebRequest)
	 */
	@Override
	protected Object createAttribute(String attributeName, MethodParameter parameter, WebDataBinderFactory binderFactory,
			NativeWebRequest request) throws Exception {

		MapDataBinder binder = new MapDataBinder(parameter.getParameterType(), conversionService.getObject());
		binder.bind(new MutablePropertyValues(request.getParameterMap()));

		return proxyFactory.createProjection(parameter.getParameterType(), binder.getTarget());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.annotation.ModelAttributeMethodProcessor#bindRequestParameters(org.springframework.web.bind.WebDataBinder, org.springframework.web.context.request.NativeWebRequest)
	 */
	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {}
}
