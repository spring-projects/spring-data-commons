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
package org.springframework.data.web;

import org.jspecify.annotations.NonNull;

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
 * controller methods. Request properties to be parsed can be configured defaulting to {@code page} for the page number
 * and {@code size} for the page size.
 * <p>
 * Parameters can be {@link #setPrefix(String) prefixed} to disambiguate from other parameters in the request if
 * necessary.
 *
 * @since 2.2
 * @author Mark Paluch
 * @author Yanming Zhou
 */
public class ReactivePageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	private static final ReactiveSortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new ReactiveSortHandlerMethodArgumentResolver();

	private final ReactiveSortHandlerMethodArgumentResolver sortResolver;

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

		Assert.notNull(sortResolver, "ReactiveSortHandlerMethodArgumentResolver must not be null");

		this.sortResolver = sortResolver;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Pageable.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public Pageable resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		String page = queryParams.getFirst(getParameterNameToUse(getPageParameterName(), parameter));
		String pageSize = queryParams.getFirst(getParameterNameToUse(getSizeParameterName(), parameter));

		Sort sort = sortResolver.resolveArgumentValue(parameter, bindingContext, exchange);
		Pageable pageable = getPageable(parameter, page, pageSize);

		if (!sort.isSorted()) {
			return pageable;
		}

		return pageable.isPaged()
				? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
				: Pageable.unpaged(sort);
	}
}
