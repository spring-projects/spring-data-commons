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

import java.util.Arrays;
import java.util.Map.Entry;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
 * @author Oliver Gierke
 * @since 1.11
 */
public class QuerydslPredicateArgumentResolver implements HandlerMethodArgumentResolver {

	private final QuerydslBindingsFactory bindingsFactory;
	private final QuerydslPredicateBuilder predicateBuilder;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given {@link ConversionService}.
	 * 
	 * @param factory
	 * @param conversionService defaults to {@link DefaultConversionService} if {@literal null}.
	 */
	public QuerydslPredicateArgumentResolver(QuerydslBindingsFactory factory, ConversionService conversionService) {

		this.bindingsFactory = factory;
		this.predicateBuilder = new QuerydslPredicateBuilder(
				conversionService == null ? new DefaultConversionService() : conversionService,
				factory.getEntityPathResolver());
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
			throw new IllegalArgumentException(String.format("Parameter at position %s must be of type Predicate but was %s.",
					parameter.getParameterIndex(), parameter.getParameterType()));
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Override
	public Predicate resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();

		for (Entry<String, String[]> entry : webRequest.getParameterMap().entrySet()) {
			parameters.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}

		QuerydslPredicate annotation = parameter.getParameterAnnotation(QuerydslPredicate.class);
		TypeInformation<?> domainType = extractTypeInfo(parameter).getActualType();

		Class<? extends QuerydslBinderCustomizer> customizer = annotation == null ? null : annotation.bindings();
		QuerydslBindings bindings = bindingsFactory.createBindingsFor(customizer, domainType);

		return predicateBuilder.getPredicate(domainType, parameters, bindings);
	}

	/**
	 * Obtains the domain type information from the given method parameter. Will favor an explicitly registered on through
	 * {@link QuerydslPredicate#root()} but use the actual type of the method's return type as fallback.
	 * 
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	static TypeInformation<?> extractTypeInfo(MethodParameter parameter) {

		QuerydslPredicate annotation = parameter.getParameterAnnotation(QuerydslPredicate.class);

		if (annotation != null && !Object.class.equals(annotation.root())) {
			return ClassTypeInformation.from(annotation.root());
		}

		return detectDomainType(ClassTypeInformation.fromReturnTypeOf(parameter.getMethod()));
	}

	private static TypeInformation<?> detectDomainType(TypeInformation<?> source) {

		if (source.getTypeArguments().isEmpty()) {
			return source;
		}

		TypeInformation<?> actualType = source.getActualType();

		if (source != actualType) {
			return detectDomainType(actualType);
		}

		if (source instanceof Iterable) {
			return source;
		}

		return detectDomainType(source.getComponentType());
	}
}
