/*
 * Copyright 2023-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Reactive {@link HandlerMethodArgumentResolver} to create {@link OffsetScrollPosition} instances from query string
 * parameters.
 *
 * @author Yanming Zhou
 * @since 3.2
 */
public class ReactiveOffsetScrollPositionHandlerMethodArgumentResolver
		extends OffsetScrollPositionHandlerMethodArgumentResolverSupport implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return OffsetScrollPosition.class.equals(parameter.nestedIfOptional().getNestedParameterType());
	}

	@Override
	public @Nullable Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		List<String> offsetParameter = exchange.getRequest().getQueryParams().get(getOffsetParameter(parameter));

		return adaptArgumentIfNecessary(parseParameterIntoOffsetScrollPosition(offsetParameter), parameter);
	}
}
