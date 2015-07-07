/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web.querydsl;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.mysema.query.types.Predicate;

/**
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateArgumentResolver implements HandlerMethodArgumentResolver {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		return parameter.hasParameterAnnotation(QueryDslPredicate.class)
				&& ClassUtils.isAssignable(Predicate.class, parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		MutablePropertyValues properties = new MutablePropertyValues(webRequest.getParameterMap());

		return new QueryDslPredicateAccessor(extractTypeInfo(parameter), extractPathSpecificPredicateBuilders(parameter))
				.getPredicate(properties);
	}

	private TypeInformation<?> extractTypeInfo(MethodParameter parameter) {

		Class<?> type = parameter.getParameterAnnotation(QueryDslPredicate.class).root();

		return type == Object.class ? ClassTypeInformation.fromReturnTypeOf(parameter.getMethod()) : ClassTypeInformation
				.from(type);
	}

	private QueryDslPredicateSpecification extractPathSpecificPredicateBuilders(MethodParameter parameter)
			throws InstantiationException, IllegalAccessException {

		QueryDslSpecification spec = parameter.getMethodAnnotation(QueryDslSpecification.class);

		Class<? extends QueryDslPredicateSpecification> specType = spec != null ? spec.spec()
				: QueryDslPredicateSpecification.class;
		return specType.newInstance();
	}
}
