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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.querydsl.core.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link com.querydsl.core.types.Predicate} into Spring MVC
 * controller methods.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Matías Hermosilla
 * @author Mark Paluch
 * @since 1.11
 */
public class QuerydslPredicateArgumentResolver extends QuerydslPredicateArgumentResolverSupport
		implements HandlerMethodArgumentResolver {

	/**
	 * Create a new {@link QuerydslPredicateArgumentResolver}.
	 *
	 * @param factory the {@link QuerydslBindingsFactory} to use, must not be {@literal null}.
	 * @param conversionService the optional {@link ConversionService} to use, must not be {@literal null}. Defaults to
	 *          {@link DefaultConversionService} if {@link Optional#empty() empty}.
	 */
	public QuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory,
			Optional<ConversionService> conversionService) {
		super(factory, conversionService.orElseGet(DefaultConversionService::getSharedInstance));
	}

	/**
	 * Create a new {@link QuerydslPredicateArgumentResolver}.
	 *
	 * @param factory the {@link QuerydslBindingsFactory} to use, must not be {@literal null}.
	 * @param conversionService the {@link ConversionService} to use, must not be {@literal null}.
	 * @since 2.5
	 */
	public QuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory, ConversionService conversionService) {
		super(factory, conversionService);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Nullable
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		MultiValueMap<String, String> queryParameters = getQueryParameters(webRequest);
		Predicate result = getPredicate(parameter, queryParameters);

		return potentiallyConvertMethodParameterValue(parameter, result);
	}

	private static MultiValueMap<String, String> getQueryParameters(NativeWebRequest webRequest) {

		Map<String, String[]> parameterMap = webRequest.getParameterMap();
		MultiValueMap<String, String> queryParameters = new LinkedMultiValueMap<>(parameterMap.size());

		for (Entry<String, String[]> entry : parameterMap.entrySet()) {
			queryParameters.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}

		return queryParameters;
	}

}
