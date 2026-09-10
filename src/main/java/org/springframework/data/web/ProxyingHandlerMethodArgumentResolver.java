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

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;

/**
 * {@link HandlerMethodArgumentResolver} to create Proxy instances for interface-based controller method parameters.
 * Resolution requires the method parameter or its interface type to be annotated with {@link ProjectedPayload}.
 * <p>
 * By default, data binding for collection properties is limited to {@code 1024} elements. This limit can be overridden
 * by setting the {@value #COLLECTION_SIZE_LIMIT_PARAM} Spring property.
 *
 * @author Oliver Gierke
 * @author Chris Bono
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Seonggon Cho
 * @since 1.10
 */
public class ProxyingHandlerMethodArgumentResolver extends ModelAttributeMethodProcessor
		implements BeanFactoryAware, BeanClassLoaderAware {

	/**
	 * Name of the Spring property to configure the collection size limit for data binding.
	 */
	public static final String COLLECTION_SIZE_LIMIT_PARAM = "spring.data.web.projection.collection-limit";

	private final SpelAwareProxyProjectionFactory proxyFactory;
	private final ObjectFactory<ConversionService> conversionService;
	private final int collectionSizeLimit;

	/**
	 * Creates a new {@link ProxyingHandlerMethodArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param conversionService must not be {@literal null}.
	 * @param annotationNotRequired whether interface-based method arguments are considered without requiring a
	 *          {@code @ModelAttribute} annotation.
	 */
	public ProxyingHandlerMethodArgumentResolver(ObjectFactory<ConversionService> conversionService,
			boolean annotationNotRequired) {

		super(annotationNotRequired);

		this.proxyFactory = new SpelAwareProxyProjectionFactory();
		this.conversionService = conversionService;

		String sizeFromProperty = SpringProperties.getProperty(COLLECTION_SIZE_LIMIT_PARAM);
		this.collectionSizeLimit = StringUtils.hasText(sizeFromProperty)
				? NumberUtils.parseNumber(sizeFromProperty, Integer.class)
				: MapDataBinder.DEFAULT_COLLECTION_LIMIT;
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
		return parameter.hasParameterAnnotation(ProjectedPayload.class)
				|| AnnotatedElementUtils.findMergedAnnotation(type, ProjectedPayload.class) != null;
	}

	@Override
	protected Object createAttribute(String attributeName, MethodParameter parameter, WebDataBinderFactory binderFactory,
			NativeWebRequest request) throws Exception {

		MapDataBinder binder = new MapDataBinder(parameter.getParameterType(), conversionService.getObject(),
				collectionSizeLimit);
		binder.bind(getPropertyValues(request));

		return proxyFactory.createProjection(parameter.getParameterType(), binder.getTarget());
	}

	/**
	 * Returns the request parameters and, in case of a multipart request, the uploaded files of the given
	 * {@link NativeWebRequest} as {@link MutablePropertyValues}. Files are bound the same way
	 * {@link org.springframework.web.bind.WebDataBinder} binds them: a single file as is, multiple files for the same
	 * name as {@link java.util.List}.
	 *
	 * @param request must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	private static MutablePropertyValues getPropertyValues(NativeWebRequest request) {

		MutablePropertyValues values = new MutablePropertyValues(request.getParameterMap());
		MultipartRequest multipartRequest = request.getNativeRequest(MultipartRequest.class);

		if (multipartRequest != null) {
			multipartRequest.getMultiFileMap()
					.forEach((name, files) -> values.add(name, files.size() == 1 ? files.get(0) : files));
		}

		return values;
	}

	@Override
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {}

}
