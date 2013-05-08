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
package org.springframework.data.web;

import static org.springframework.data.web.SpringDataAnnotationUtils.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extracts paging information from web requests and thus allows injecting {@link Pageable} instances into controller
 * methods. Request properties to be parsed can be configured. Default configuration uses request properties beginning
 * with {@link #PAGE_PROPERTY}{@link #DEFAULT_SEPARATOR}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public class PageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver, UriComponentsContributor {

	/**
	 * A {@link PageableHandlerMethodArgumentResolver} preconfigured to the setup of {@link PageableArgumentResolver}. Use
	 * that if you need to stick to the former request parameters an 1-indexed behavior. This will be removed in the next
	 * major version (1.7). So consider migrating to the new way of exposing request parameters.
	 */
	@Deprecated
	public static final PageableHandlerMethodArgumentResolver LEGACY;

	static {
		LEGACY = new PageableHandlerMethodArgumentResolver();
		LEGACY.pageProperty = "page.page";
		LEGACY.sizeProperty = "page.size";
		LEGACY.fallbackPageable = new PageRequest(1, 10);
		LEGACY.oneIndexedParameters = true;
		LEGACY.sortResolver.setLegacyMode(true);
		LEGACY.sortResolver.setSortParameter("page.sort");
	}

	private static final Pageable DEFAULT_PAGE_REQUEST = new PageRequest(0, 20);
	private static final String DEFAULT_PAGE_PROPERTY = "page";
	private static final String DEFAULT_SIZE_PROPERTY = "size";
	private static final String DEFAULT_PREFIX = "";
	private static final String DEFAULT_QUALIFIER_SEPARATOR = "_";

	private Pageable fallbackPageable = DEFAULT_PAGE_REQUEST;
	private SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
	private String pageProperty = DEFAULT_PAGE_PROPERTY;
	private String sizeProperty = DEFAULT_SIZE_PROPERTY;
	private String prefix = DEFAULT_PREFIX;
	private String qualifierSeparator = DEFAULT_QUALIFIER_SEPARATOR;
	private boolean oneIndexedParameters = false;

	/**
	 * Configures the {@link Pageable} to be used as fallback in case no {@link PageableDefault} or
	 * {@link PageableDefaults} (the latter only supported in legacy mode) can be found at the method parameter to be
	 * resolved.
	 * <p>
	 * If you set this to {@literal null}, be aware that you controller methods will get {@literal null} handed into them
	 * in case no {@link Pageable} data can be found in the request.
	 * 
	 * @param fallbackPageable the {@link Pageable} to be used as general fallback.
	 */
	public void setFallbackPageable(Pageable fallbackPageable) {
		this.fallbackPageable = fallbackPageable;
	}

	/**
	 * Configures the parameter name to be used to find the page number in the request. Defaults to {@code page}.
	 * 
	 * @param pageProperty the parameter name to be used, must not be {@literal null} or empty.
	 */
	public void setPageProperty(String pageProperty) {

		Assert.hasText(pageProperty, "Page parameter name must not be null or empty!");
		this.pageProperty = pageProperty;
	}

	/**
	 * Configures the parameter name to be used to find the page size in the request. Defaults to {@code size}.
	 * 
	 * @param sizeProperty the parameter name to be used, must not be {@literal null} or empty.
	 */
	public void setSizeProperty(String sizeProperty) {

		Assert.hasText(sizeProperty, "Size parameter name must not be null or empty!");
		this.sizeProperty = sizeProperty;
	}

	/**
	 * Configures a general prefix to be prepended to the page number and page size parameters. Useful to namespace the
	 * property names used in case they are clashing with ones used by your application. By default, no prefix is used.
	 * 
	 * @param prefix the prefix to be used or {@literal null} to reset to the default.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
	}

	/**
	 * The separator to be used between the qualifier and the actual page number and size properties. Defaults to
	 * {@code _}. So a qualifier of {@code foo} will result in a page number parameter of {@code foo_page}.
	 * 
	 * @param qualifierSeparator the qualifierSeparator to be used or {@literal null} to reset to the default.
	 */
	public void setQualifierSeparator(String qualifierSeparator) {
		this.qualifierSeparator = qualifierSeparator == null ? DEFAULT_QUALIFIER_SEPARATOR : qualifierSeparator;
	}

	/**
	 * Configure the {@link SortHandlerMethodArgumentResolver} to be used with the
	 * {@link PageableHandlerMethodArgumentResolver}.
	 * 
	 * @param sortResolver the {@link SortHandlerMethodArgumentResolver} to be used ot {@literal null} to reset it to the
	 *          default one.
	 */
	public void setSortResolver(SortHandlerMethodArgumentResolver sortResolver) {
		this.sortResolver = sortResolver == null ? new SortHandlerMethodArgumentResolver() : sortResolver;
	}

	/**
	 * Configures whether to expose and assume 1-based page number indexes in the request parameters. Defaults to
	 * {@literal false}, meaning a page number of 0 in the request equals the first page. If this is set to
	 * {@literal true}, a page number of 1 in the request will be considered the first page.
	 * 
	 * @param oneIndexedParameters the oneIndexedParameters to set
	 */
	public void setOneIndexedParameters(boolean oneIndexedParameters) {
		this.oneIndexedParameters = oneIndexedParameters;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.equals(parameter.getParameterType());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.mvc.UriComponentsContributor#enhance(org.springframework.web.util.UriComponentsBuilder, org.springframework.core.MethodParameter, java.lang.Object)
	 */
	public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {

		if (!(value instanceof Pageable)) {
			return;
		}

		Pageable pageable = (Pageable) value;

		String pagePropertyName = getParameterNameToUse(pageProperty, parameter);
		String propertyToLookup = getParameterNameToUse(sizeProperty, parameter);

		int pageNumber = pageable.getPageNumber();

		builder.queryParam(pagePropertyName, oneIndexedParameters ? pageNumber + 1 : pageNumber);
		builder.queryParam(propertyToLookup, pageable.getPageSize());

		sortResolver.enhance(builder, parameter, pageable.getSort());
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		assertPageableUniqueness(methodParameter);

		Pageable defaultOrFallback = getDefaultFromAnnotationOrFallback(methodParameter);

		String pageString = webRequest.getParameter(getParameterNameToUse(pageProperty, methodParameter));
		String pageSizeString = webRequest.getParameter(getParameterNameToUse(sizeProperty, methodParameter));

		int page = StringUtils.hasText(pageString) ? Integer.parseInt(pageString) - (oneIndexedParameters ? 1 : 0)
				: defaultOrFallback.getPageNumber();
		int pageSize = StringUtils.hasText(pageSizeString) ? Integer.parseInt(pageSizeString) : defaultOrFallback
				.getPageSize();

		Sort sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
		return new PageRequest(page, pageSize, sort == null ? defaultOrFallback.getSort() : sort);
	}

	/**
	 * Returns the name of the request parameter to find the {@link Pageable} information in. Inspects the given
	 * {@link MethodParameter} for {@link Qualifier} present and prefixes the given source parameter name with it.
	 * 
	 * @param source the basic parameter name.
	 * @param parameter the {@link MethodParameter} potentially qualified.
	 * @return
	 */
	private String getParameterNameToUse(String source, MethodParameter parameter) {

		StringBuilder builder = new StringBuilder(prefix);

		if (parameter != null && parameter.hasParameterAnnotation(Qualifier.class)) {
			builder.append(parameter.getParameterAnnotation(Qualifier.class).value());
			builder.append(qualifierSeparator);
		}

		return builder.append(source).toString();
	}

	private Pageable getDefaultFromAnnotationOrFallback(MethodParameter methodParameter) {

		if (sortResolver.legacyMode && methodParameter.hasParameterAnnotation(PageableDefaults.class)) {
			Pageable pageable = PageableArgumentResolver.getDefaultPageRequestFrom(methodParameter
					.getParameterAnnotation(PageableDefaults.class));
			return new PageRequest(pageable.getPageNumber() - 1, pageable.getPageSize(), pageable.getSort());
		}

		if (methodParameter.hasParameterAnnotation(PageableDefault.class)) {
			return getDefaultPageRequestFrom(methodParameter.getParameterAnnotation(PageableDefault.class));
		}

		return fallbackPageable;
	}

	private static Pageable getDefaultPageRequestFrom(PageableDefault defaults) {

		Integer defaultPageNumber = defaults.page();
		Integer defaultPageSize = getSpecificPropertyOrDefaultFromValue(defaults, "size");

		if (defaults.sort().length == 0) {
			return new PageRequest(defaultPageNumber, defaultPageSize);
		}

		return new PageRequest(defaultPageNumber, defaultPageSize, defaults.direction(), defaults.sort());
	}
}
