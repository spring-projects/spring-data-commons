/*
 * Copyright 2015-present the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

/**
 * {@link HandlerMethodArgumentResolver} to create Proxy instances for interface based controller method parameters.
 *
 * @author Oliver Gierke
 * @author Chris Bono
 * @author Mark Paluch
 * @since 1.10
 */
public class ProxyingHandlerMethodArgumentResolver extends ModelAttributeMethodProcessor
		implements BeanFactoryAware, BeanClassLoaderAware {

	// NonFinalForTesting
	private static LogAccessor LOGGER = new LogAccessor(ProxyingHandlerMethodArgumentResolver.class);

	private static final List<String> IGNORED_PACKAGES = List.of("java", "org.springframework");

	private final SpelAwareProxyProjectionFactory proxyFactory;
	private final ObjectFactory<ConversionService> conversionService;
	private final ProjectedPayloadDeprecationLogger deprecationLogger = new ProjectedPayloadDeprecationLogger();

	/**
	 * Creates a new {@link ProxyingHandlerMethodArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public ProxyingHandlerMethodArgumentResolver(ObjectFactory<ConversionService> conversionService,
			boolean annotationNotRequired) {

		super(annotationNotRequired);

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

		// Simple type or not annotated with @ModelAttribute (and flag set to require annotation)
		if (!super.supportsParameter(parameter)) {
			return false;
		}

		Class<?> type = parameter.getParameterType();

		// Only interfaces can be proxied
		if (!type.isInterface()) {
			return false;
		}

		// Multipart not currently supported
		if (MultipartResolutionDelegate.isMultipartArgument(parameter)) {
			return false;
		}

		// Type or parameter explicitly annotated with @ProjectedPayload
		if (parameter.hasParameterAnnotation(ProjectedPayload.class) || AnnotatedElementUtils.findMergedAnnotation(type,
				ProjectedPayload.class) != null) {
			return true;
		}

		// Parameter annotated with @ModelAttribute
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			this.deprecationLogger.logDeprecationForParameter(parameter);
			return false;
		}

		// Exclude any other parameters annotated with Spring annotation
		if (Arrays.stream(parameter.getParameterAnnotations())
				.map(Annotation::annotationType)
				.map(Class::getPackageName)
				.anyMatch(it -> it.startsWith("org.springframework"))) {

			return false;
		}

		// Fallback for only user defined interfaces
		String packageName = ClassUtils.getPackageName(type);
		if (IGNORED_PACKAGES.stream().noneMatch(packageName::startsWith)) {
			this.deprecationLogger.logDeprecationForParameter(parameter);
			return false;
		}

		return false;
	}

	@Override
	protected Object createAttribute(String attributeName, MethodParameter parameter, WebDataBinderFactory binderFactory,
			NativeWebRequest request) throws Exception {

		MapDataBinder binder = new MapDataBinder(parameter.getParameterType(), conversionService.getObject());
		binder.bind(new MutablePropertyValues(request.getParameterMap()));

		return proxyFactory.createProjection(parameter.getParameterType(), binder.getTarget());
	}

	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {}

	/**
	 * Logs a warning message when a parameter is proxied but not explicitly annotated with {@link @ProjectedPayload}.
	 * <p>
	 * To avoid log spamming, the message is only logged the first time the parameter is encountered.
	 */
	static class ProjectedPayloadDeprecationLogger {

		private static final String MESSAGE = "Parameter %sat index %s in [%s] is not annotated with @ProjectedPayload. Make sure to annotate it with @ProjectedPayload (at the parameter or type level) to use it for projections.";

		private final Set<MethodParameter> loggedParameters = Collections.synchronizedSet(new HashSet<>());

		/**
		 * Log a warning the first time a non-annotated method parameter is encountered.
		 *
		 * @param parameter the parameter
		 */
		void logDeprecationForParameter(MethodParameter parameter) {

			if (!this.loggedParameters.add(parameter)) {
				return;
			}

			String paramName = parameter.getParameterName();
			String paramNameOrEmpty = paramName != null ? ("'" + paramName + "' ") : "";
			String methodName = parameter.getMethod() != null ? parameter.getMethod().toGenericString() : "constructor";

			LOGGER.warn(() -> MESSAGE.formatted(paramNameOrEmpty, parameter.getParameterIndex(), methodName));
		}

	}

}
