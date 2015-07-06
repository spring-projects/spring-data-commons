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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.mysema.query.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link com.mysema.query.types.Predicate} into Spring MVC
 * controller methods.
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
public class QuerydslPredicateArgumentResolver implements HandlerMethodArgumentResolver {

	private final QuerydslPredicateBuilder predicateBuilder;
	private final ConversionService conversionService;

	/**
	 * @param conversionService Defaulted to {@link DefaultConversionService} if {@literal null}.
	 */
	public QuerydslPredicateArgumentResolver(ConversionService conversionService) {

		this.conversionService = conversionService == null ? new DefaultConversionService() : conversionService;
		this.predicateBuilder = new QuerydslPredicateBuilder();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {

		if (Predicate.class.equals(parameter.getParameterType())) {
			return true;
		}

		if (parameter.hasParameterAnnotation(QuerydslPredicate.class)) {
			throw new IllegalArgumentException(String.format(
					"Parameter at position %s must be of type Predicate but was %s.", parameter.getParameterIndex(),
					parameter.getParameterType()));
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		return predicateBuilder.getPredicate(new MutablePropertyValues(webRequest.getParameterMap()),
				createBindingContext(parameter));
	}

	private QuerydslBindingContext createBindingContext(MethodParameter parameter) throws InstantiationException,
			IllegalAccessException {
		return new QuerydslBindingContext(extractTypeInfo(parameter), extractBindings(parameter), conversionService);
	}

	private TypeInformation<?> extractTypeInfo(MethodParameter parameter) {

		Class<?> type = parameter.getParameterAnnotation(QuerydslPredicate.class).root();

		return type == Object.class ? ClassTypeInformation.fromReturnTypeOf(parameter.getMethod()) : ClassTypeInformation
				.from(type);
	}

	private QuerydslBindings extractBindings(MethodParameter parameter) throws InstantiationException,
			IllegalAccessException {

		return BeanUtils.instantiateClass(parameter.getParameterAnnotation(QuerydslPredicate.class).bindings());
	}
}
