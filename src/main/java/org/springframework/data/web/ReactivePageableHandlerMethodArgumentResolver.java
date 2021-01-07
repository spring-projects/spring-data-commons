/*
 * Copyright 2017-2021 the original author or authors.
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

import javax.annotation.Nonnull;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Extracts paging information from web requests and thus allows injecting {@link Pageable} instances into WebFlux
 * controller methods. Request properties to be parsed can be configured. Default configuration uses request parameters
 * beginning with {@link #DEFAULT_PAGE_PARAMETER}{@link #DEFAULT_QUALIFIER_DELIMITER}.
 *
 * @since 2.2
 * @author Mark Paluch
 */
public class ReactivePageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	private static final ReactiveSortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new ReactiveSortHandlerMethodArgumentResolver();

	private ReactiveSortHandlerMethodArgumentResolver sortResolver;

	/**
	 * Constructs an instance of this resolved with a default {@link ReactiveSortHandlerMethodArgumentResolver}.
	 */
	public ReactivePageableHandlerMethodArgumentResolver() {
		this(DEFAULT_SORT_RESOLVER);
	}

	/**
	 * Constructs an instance of this resolver with the specified {@link SortArgumentResolver}.
	 *
	 * @param sortResolver the sort resolver to use.
	 */
	public ReactivePageableHandlerMethodArgumentResolver(ReactiveSortHandlerMethodArgumentResolver sortResolver) {

		Assert.notNull(sortResolver, "ReactiveSortHandlerMethodArgumentResolver must not be null!");

		this.sortResolver = sortResolver;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.equals(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver#resolveArgumentValue(org.springframework.core.MethodParameter, org.springframework.web.reactive.BindingContext, org.springframework.web.server.ServerWebExchange)
	 */
	@Nonnull
	@Override
	public Pageable resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		String page = queryParams.getFirst(getParameterNameToUse(getPageParameterName(), parameter));
		String pageSize = queryParams.getFirst(getParameterNameToUse(getSizeParameterName(), parameter));

		Sort sort = sortResolver.resolveArgumentValue(parameter, bindingContext, exchange);

		Pageable pageable = getPageable(parameter, page, pageSize);

		return sort.isSorted() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort) : pageable;
	}
}
