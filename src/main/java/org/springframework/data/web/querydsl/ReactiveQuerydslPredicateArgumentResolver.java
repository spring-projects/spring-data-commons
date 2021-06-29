/*
 * Copyright 2021 the original author or authors.
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

import java.util.List;
import java.util.Map.Entry;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.lang.Nullable;
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver(org.springframework.core.MethodParameter, org.springframework.web.reactive.BindingContext, org.springframework.web.server.ServerWebExchange)
	 */
	@Override
	@Nullable
	public Object resolveArgumentValue(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParameters = getQueryParameters(exchange);
		Predicate result = getPredicate(parameter, queryParameters);

		return potentiallyConvertMethodParameterValue(parameter, result);
	}


	private static MultiValueMap<String, String> getQueryParameters(ServerWebExchange exchange) {

		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(queryParams.size());

		for (Entry<String, List<String>> entry : queryParams.entrySet()) {
			parameters.put(entry.getKey(), entry.getValue());
		}

		return parameters;
	}

}
