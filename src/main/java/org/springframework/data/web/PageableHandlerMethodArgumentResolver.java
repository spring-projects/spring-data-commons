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
package org.springframework.data.web;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Extracts paging information from web requests and thus allows injecting {@link Pageable} instances into controller
 * methods. Request properties to be parsed can be configured. Default configuration uses request parameters beginning
 * with {@link #DEFAULT_PAGE_PARAMETER}{@link #DEFAULT_QUALIFIER_DELIMITER}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public class PageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolverSupport
		implements PageableArgumentResolver {

	private static final SortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new SortHandlerMethodArgumentResolver();
	private SortArgumentResolver sortResolver;

	/**
	 * Constructs an instance of this resolved with a default {@link SortHandlerMethodArgumentResolver}.
	 */
	public PageableHandlerMethodArgumentResolver() {
		this((SortArgumentResolver) null);
	}

	/**
	 * Constructs an instance of this resolver with the specified {@link SortHandlerMethodArgumentResolver}.
	 *
	 * @param sortResolver the sort resolver to use
	 */
	public PageableHandlerMethodArgumentResolver(SortHandlerMethodArgumentResolver sortResolver) {
		this((SortArgumentResolver) sortResolver);
	}

	/**
	 * Constructs an instance of this resolver with the specified {@link SortArgumentResolver}.
	 *
	 * @param sortResolver the sort resolver to use
	 * @since 1.13
	 */
	public PageableHandlerMethodArgumentResolver(@Nullable SortArgumentResolver sortResolver) {
		this.sortResolver = sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.equals(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Pageable resolveArgument(MethodParameter methodParameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		String page = webRequest.getParameter(getParameterNameToUse(getPageParameterName(), methodParameter));
		String pageSize = webRequest.getParameter(getParameterNameToUse(getSizeParameterName(), methodParameter));

		Sort sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
		Pageable pageable = getPageable(methodParameter, page, pageSize);

		if (sort.isSorted()) {
			return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
		}

		return pageable;
	}
}
