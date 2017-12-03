/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to automatically create {@link Sort} instances from request parameters or
 * {@link SortDefault} annotations.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public class SortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolverSupport {

	private static final String DEFAULT_PARAMETER = "sort";
	private static final String DEFAULT_PROPERTY_DELIMITER = ",";
	private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
	private static final Sort DEFAULT_SORT = Sort.unsorted();

	private static final String SORT_DEFAULTS_NAME = SortDefault.SortDefaults.class.getSimpleName();
	private static final String SORT_DEFAULT_NAME = SortDefault.class.getSimpleName();

	private Sort fallbackSort = DEFAULT_SORT;
	private String sortParameter = DEFAULT_PARAMETER;
	private String propertyDelimiter = DEFAULT_PROPERTY_DELIMITER;
	private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;

	/**
	 * Configure the request parameter to lookup sort information from. Defaults to {@code sort}.
	 *
	 * @param sortParameter must not be {@literal null} or empty.
	 */
	public void setSortParameter(String sortParameter) {

		Assert.hasText(sortParameter, "SortParameter must not be null nor empty!");
		this.sortParameter = sortParameter;
	}

	/**
	 * Configures the delimiter used to separate property references and the direction to be sorted by. Defaults to
	 * {@code}, which means sort values look like this: {@code firstname,lastname,asc}.
	 *
	 * @param propertyDelimiter must not be {@literal null} or empty.
	 */
	public void setPropertyDelimiter(String propertyDelimiter) {

		Assert.hasText(propertyDelimiter, "Property delimiter must not be null or empty!");
		this.propertyDelimiter = propertyDelimiter;
	}

	/**
	 * Configures the delimiter used to separate the qualifier from the sort parameter. Defaults to {@code _}, so a
	 * qualified sort property would look like {@code qualifier_sort}.
	 *
	 * @param qualifierDelimiter the qualifier delimiter to be used or {@literal null} to reset to the default.
	 */
	public void setQualifierDelimiter(String qualifierDelimiter) {
		this.qualifierDelimiter = qualifierDelimiter == null ? DEFAULT_QUALIFIER_DELIMITER : qualifierDelimiter;
	}

	/**
	 * Configures the {@link Sort} to be used as fallback in case no {@link SortDefault} or {@link SortDefaults} (the
	 * latter only supported in legacy mode) can be found at the method parameter to be resolved.
	 * <p>
	 * If you set this to {@literal null}, be aware that you controller methods will get {@literal null} handed into them
	 * in case no {@link Sort} data can be found in the request.
	 *
	 * @param fallbackSort the {@link Sort} to be used as general fallback.
	 */
	public void setFallbackSort(Sort fallbackSort) {
		this.fallbackSort = fallbackSort;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Sort.class.equals(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Sort resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		String[] directionParameter = webRequest.getParameterValues(getSortParameter(parameter));

		// No parameter
		if (directionParameter == null) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		// Single empty parameter, e.g "sort="
		if (directionParameter.length == 1 && !StringUtils.hasText(directionParameter[0])) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		return parseParameterIntoSort(Arrays.asList(directionParameter), getPropertyDelimiter());
	}
}
