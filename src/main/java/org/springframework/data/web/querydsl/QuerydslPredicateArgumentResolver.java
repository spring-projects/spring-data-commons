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
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.mysema.query.types.EntityPath;
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

	private static final String INVALID_DOMAIN_TYPE = "Unable to find Querydsl root type for detected domain type %s! User @%s's root attribute to define the domain type manually!";
	private final QuerydslPredicateBuilder predicateBuilder;
	private final EntityPathResolver resolver;
	private final Map<TypeInformation<?>, EntityPath<?>> entityPaths;

	private AutowireCapableBeanFactory beanFactory;
	private Repositories repositories;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given {@link ConversionService}.
	 * 
	 * @param conversionService defaults to {@link DefaultConversionService} if {@literal null}.
	 */
	public QuerydslPredicateArgumentResolver(ConversionService conversionService) {

		this.resolver = SimpleEntityPathResolver.INSTANCE;
		this.entityPaths = new ConcurrentReferenceHashMap<TypeInformation<?>, EntityPath<?>>();
		this.predicateBuilder = new QuerydslPredicateBuilder(
				conversionService == null ? new DefaultConversionService() : conversionService, resolver);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
		this.repositories = new Repositories(applicationContext);
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

		return predicateBuilder.getPredicate(domainType, parameters, createBindings(annotation, domainType));
	}

	QuerydslBindings createBindings(QuerydslPredicate annotation, TypeInformation<?> domainType) {

		EntityPath<?> path = verifyEntityPathPresent(domainType);

		QuerydslBindings bindings = new QuerydslBindings();
		findCustomizerForDomainType(annotation, domainType.getType()).customize(bindings, path);

		return bindings;
	}

	/**
	 * Obtains the {@link QuerydslBinderCustomizer} for the given domain type. Will inspect the given annotation for a
	 * dedicatedly configured one or consider the domain types's repository.
	 * 
	 * @param annotation
	 * @param domainType
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private QuerydslBinderCustomizer<EntityPath<?>> findCustomizerForDomainType(QuerydslPredicate annotation,
			Class<?> domainType) {

		if (annotation != null) {

			Class<? extends QuerydslBinderCustomizer> bindings = annotation.bindings();

			if (bindings != QuerydslBinderCustomizer.class) {
				return createQuerydslBinderCustomizer(bindings);
			}
		}

		if (repositories != null && repositories.hasRepositoryFor(domainType)) {

			Object repository = repositories.getRepositoryFor(domainType);

			if (repository instanceof QuerydslBinderCustomizer) {
				return (QuerydslBinderCustomizer<EntityPath<?>>) repository;
			}
		}

		return NoOpCustomizer.INSTANCE;
	}

	/**
	 * Obtains a {@link QuerydslBinderCustomizer} for the given type. Will try to obtain a bean from the
	 * {@link org.springframework.beans.factory.BeanFactory} first or fall back to create a fresh instance through the
	 * {@link org.springframework.beans.factory.BeanFactory} or finally falling back to a plain instantiation if no
	 * {@link org.springframework.beans.factory.BeanFactory} is present.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private QuerydslBinderCustomizer<EntityPath<?>> createQuerydslBinderCustomizer(
			Class<? extends QuerydslBinderCustomizer> type) {

		if (beanFactory == null) {
			return BeanUtils.instantiateClass(type);
		}

		try {
			return beanFactory.getBean(type);
		} catch (NoSuchBeanDefinitionException e) {
			return beanFactory.createBean(type);
		}
	}

	/**
	 * Tries to detect a Querydsl query type for the given domain type candidate via the configured
	 * {@link EntityPathResolver}.
	 * 
	 * @param candidate must not be {@literal null}.
	 * @throws IllegalStateException to indicate the query type can't be found and manual configuration is necessary.
	 */
	private EntityPath<?> verifyEntityPathPresent(TypeInformation<?> candidate) {

		EntityPath<?> path = entityPaths.get(candidate);

		if (path != null) {
			return path;
		}

		Class<?> type = candidate.getType();

		try {
			path = resolver.createPath(type);
		} catch (IllegalArgumentException o_O) {
			throw new IllegalStateException(
					String.format(INVALID_DOMAIN_TYPE, candidate.getType(), QuerydslPredicate.class.getSimpleName()), o_O);
		}

		entityPaths.put(candidate, path);
		return path;
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

	private static enum NoOpCustomizer implements QuerydslBinderCustomizer<EntityPath<?>> {

		INSTANCE;

		@Override
		public void customize(QuerydslBindings bindings, EntityPath<?> root) {}
	}
}
