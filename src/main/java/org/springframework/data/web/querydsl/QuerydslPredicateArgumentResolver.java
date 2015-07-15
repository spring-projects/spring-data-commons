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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
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
public class QuerydslPredicateArgumentResolver implements HandlerMethodArgumentResolver, ApplicationContextAware {

	private static final QuerydslBindings DEFAULT_BINDINGS = new QuerydslBindings();

	private final QuerydslPredicateBuilder predicateBuilder;

	private AutowireCapableBeanFactory beanFactory;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given {@link ConversionService}.
	 * 
	 * @param conversionService defaults to {@link DefaultConversionService} if {@literal null}.
	 */
	public QuerydslPredicateArgumentResolver(ConversionService conversionService) {
		this.predicateBuilder = new QuerydslPredicateBuilder(
				conversionService == null ? new DefaultConversionService() : conversionService);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
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

		return predicateBuilder.getPredicate(parameters, createBindings(parameter),
				extractTypeInfo(parameter).getActualType());
	}

	private TypeInformation<?> extractTypeInfo(MethodParameter parameter) {

		QuerydslPredicate annotation = parameter.getParameterAnnotation(QuerydslPredicate.class);

		if (annotation == null || Object.class.equals(annotation.root())) {
			return ClassTypeInformation.fromReturnTypeOf(parameter.getMethod());
		}

		return ClassTypeInformation.from(annotation.root());
	}

	private QuerydslBindings createBindings(MethodParameter parameter)
			throws InstantiationException, IllegalAccessException {

		QuerydslPredicate annotation = parameter.getParameterAnnotation(QuerydslPredicate.class);

		if (annotation == null) {
			return DEFAULT_BINDINGS;
		}

		Class<? extends QuerydslBindings> type = annotation.bindings();
		return beanFactory != null ? beanFactory.createBean(type) : BeanUtils.instantiateClass(type);
	}
}
