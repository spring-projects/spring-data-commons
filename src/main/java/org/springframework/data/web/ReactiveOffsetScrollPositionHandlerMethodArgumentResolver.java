/*
 * Copyright 2017-2023 the original author or authors.
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
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * Reactive {@link HandlerMethodArgumentResolver} to create {@link OffsetScrollPosition} instances from query string parameters.
 *
 * @since 3.2
 * @author Yanming Zhou
 */
public class ReactiveOffsetScrollPositionHandlerMethodArgumentResolver extends OffsetScrollPositionHandlerMethodArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return OffsetScrollPosition.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public OffsetScrollPosition resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		List<String> offsetParameter = exchange.getRequest().getQueryParams().get(getOffsetParameter(parameter));

		return parseParameterIntoOffsetScrollPosition(offsetParameter);
	}
}
