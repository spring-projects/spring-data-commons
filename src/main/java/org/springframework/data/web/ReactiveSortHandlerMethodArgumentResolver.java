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

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Reactive {@link HandlerMethodArgumentResolver} to create {@link Sort} instances from query string parameters or
 * {@link SortDefault} annotations.
 *
 * @since 2.2
 * @author Mark Paluch
 */
public class ReactiveSortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Sort.class.equals(parameter.getParameterType());
	}

	/*
	 *(non-Javadoc)
	 * @see org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver#resolveArgumentValue(org.springframework.core.MethodParameter, org.springframework.web.reactive.BindingContext, org.springframework.web.server.ServerWebExchange)
	 */
	@Nonnull
	@Override
	public Sort resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		List<String> directionParameter = exchange.getRequest().getQueryParams().get(getSortParameter(parameter));

		// No parameter
		if (directionParameter == null) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		// Single empty parameter, e.g "sort="
		if (directionParameter.size() == 1 && !StringUtils.hasText(directionParameter.get(0))) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		return parseParameterIntoSort(directionParameter, getPropertyDelimiter());
	}
}
