/*
 * Copyright 2015-2021 the original author or authors.
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
import java.util.Optional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.util.CastUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link com.querydsl.core.types.Predicate} into Spring
 * WebFlux controller methods.
 *
 * @author Matías Hermosilla
 * @since 1.11
 */
public class ReactiveQuerydslPredicateArgumentResolver extends QuerydslPredicateArgumentResolverSupport
		implements HandlerMethodArgumentResolver {

	public ReactiveQuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory,
			Optional<ConversionService> conversionService) {
		super(factory, conversionService);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.springframework.web.reactive.result.method.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.reactive.BindingContext, org.springframework.web.server.ServerWebExchange)
	 */
	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
			ServerWebExchange exchange) {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

		for (Entry<String, List<String>> entry : exchange.getRequest().getQueryParams().entrySet()) {
			parameters.put(entry.getKey(), entry.getValue());
		}

		Optional<QuerydslPredicate> annotation = Optional
				.ofNullable(parameter.getParameterAnnotation(QuerydslPredicate.class));
		TypeInformation<?> domainType = extractTypeInfo(parameter).getRequiredActualType();

		Optional<Class<? extends QuerydslBinderCustomizer<?>>> bindingsAnnotation = annotation //
				.map(QuerydslPredicate::bindings) //
				.map(CastUtils::cast);

		QuerydslBindings bindings = bindingsAnnotation //
				.map(it -> bindingsFactory.createBindingsFor(domainType, it)) //
				.orElseGet(() -> bindingsFactory.createBindingsFor(domainType));

		Predicate result = predicateBuilder.getPredicate(domainType, parameters, bindings);

		if (!parameter.isOptional() && result == null) {
			return Mono.just(new BooleanBuilder());
		}

		return OPTIONAL_OF_PREDICATE.isAssignableFrom(ResolvableType.forMethodParameter(parameter)) //
				? Mono.justOrEmpty(Optional.ofNullable(result)) //
				: Mono.justOrEmpty(result);
	}

}
