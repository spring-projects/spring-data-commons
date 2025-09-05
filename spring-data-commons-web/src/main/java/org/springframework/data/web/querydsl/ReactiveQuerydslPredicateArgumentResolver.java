/*
 * Copyright 2021-2025 the original author or authors.
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
package org.springframework.data.web.querydsl;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import com.querydsl.core.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link com.querydsl.core.types.Predicate} into Spring
 * WebFlux controller methods.
 *
 * @author Matías Hermosilla
 * @author Mark Paluch
 * @since 2.5
 */
public class ReactiveQuerydslPredicateArgumentResolver extends QuerydslPredicateArgumentResolverSupport
		implements SyncHandlerMethodArgumentResolver {

	public ReactiveQuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory,
			ConversionService conversionService) {
		super(factory, conversionService);
	}

	@Override
	public @Nullable Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParameters = getQueryParameters(exchange);
		Predicate result = getPredicate(parameter, queryParameters);

		return potentiallyConvertMethodParameterValue(parameter, result);
	}


	private static MultiValueMap<String, String> getQueryParameters(ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(queryParams.size());

		parameters.putAll(queryParams);

		return parameters;
	}

}
