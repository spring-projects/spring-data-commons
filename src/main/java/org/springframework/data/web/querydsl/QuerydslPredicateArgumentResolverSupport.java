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

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.util.CastUtils;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import com.querydsl.core.types.Predicate;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link com.querydsl.core.types.Predicate} into Spring MVC
 * controller methods.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Matías Hermosilla
 * @since 2.5
 */
public abstract class QuerydslPredicateArgumentResolverSupport {

	private static final ResolvableType PREDICATE = ResolvableType.forClass(Predicate.class);

	static final ResolvableType OPTIONAL_OF_PREDICATE = ResolvableType.forClassWithGenerics(Optional.class, PREDICATE);

	protected final QuerydslBindingsFactory bindingsFactory;
	protected final QuerydslPredicateBuilder predicateBuilder;

	/**
	 * Creates a new {@link QuerydslPredicateArgumentResolver} using the given {@link ConversionService}.
	 *
	 * @param factory
	 * @param conversionService
	 */
	protected QuerydslPredicateArgumentResolverSupport(QuerydslBindingsFactory factory,
			ConversionService conversionService) {

		Assert.notNull(factory, "QuerydslBindingsFactory must not be null");
		Assert.notNull(conversionService, "ConversionService must not be null");

		this.bindingsFactory = factory;
		this.predicateBuilder = new QuerydslPredicateBuilder(conversionService, factory.getEntityPathResolver());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	public boolean supportsParameter(MethodParameter parameter) {

		ResolvableType type = ResolvableType.forMethodParameter(parameter);

		if (PREDICATE.isAssignableFrom(type) || OPTIONAL_OF_PREDICATE.isAssignableFrom(type)) {
			return true;
		}

		MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameter());

		if (annotations.isPresent(QuerydslPredicate.class)) {
			throw new IllegalArgumentException(String.format("Parameter at position %s must be of type Predicate but was %s.",
					parameter.getParameterIndex(), parameter.getParameterType()));
		}

		return false;
	}

	Predicate getPredicate(MethodParameter parameter, MultiValueMap<String, String> queryParameters) {

		MergedAnnotations annotations = MergedAnnotations.from(parameter.getParameter());
		MergedAnnotation<QuerydslPredicate> predicateAnnotation = annotations.get(QuerydslPredicate.class);

		TypeInformation<?> domainType = extractTypeInfo(parameter, predicateAnnotation).getRequiredActualType();

		Optional<Class<? extends QuerydslBinderCustomizer<?>>> bindingsAnnotation = predicateAnnotation.getValue("bindings") //
				.map(CastUtils::cast);

		QuerydslBindings bindings = bindingsAnnotation //
				.map(it -> bindingsFactory.createBindingsFor(domainType, it)) //
				.orElseGet(() -> bindingsFactory.createBindingsFor(domainType));

		return predicateBuilder.getPredicate(domainType, queryParameters, bindings);
	}

	@Nullable
	static Object potentiallyConvertMethodParameterValue(MethodParameter parameter, Predicate predicate) {

		if (!parameter.isOptional()) {
			return predicate;
		}

		if (OPTIONAL_OF_PREDICATE.isAssignableFrom(ResolvableType.forMethodParameter(parameter))) {
			return QuerydslPredicateBuilder.isEmpty(predicate) ? Optional.empty() : Optional.of(predicate);
		}

		return QuerydslPredicateBuilder.isEmpty(predicate) ? null : predicate;
	}

	/**
	 * Obtains the domain type information from the given method parameter. Will favor an explicitly registered on through
	 * {@link QuerydslPredicate#root()} but use the actual type of the method's return type as fallback.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	protected static TypeInformation<?> extractTypeInfo(MethodParameter parameter,
			MergedAnnotation<QuerydslPredicate> predicateAnnotation) {

		Optional<QuerydslPredicate> annotation = predicateAnnotation.synthesize(MergedAnnotation::isPresent);

		return annotation.filter(it -> !Object.class.equals(it.root()))//
				.<TypeInformation<?>> map(it -> ClassTypeInformation.from(it.root()))//
				.orElseGet(() -> detectDomainType(parameter));
	}

	private static TypeInformation<?> detectDomainType(MethodParameter parameter) {

		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException("Method parameter is not backed by a method!");
		}

		return detectDomainType(ClassTypeInformation.fromReturnTypeOf(method));
	}

	private static TypeInformation<?> detectDomainType(TypeInformation<?> source) {

		if (source.getTypeArguments().isEmpty()) {
			return source;
		}

		TypeInformation<?> actualType = source.getActualType();

		if (actualType == null) {
			throw new IllegalArgumentException(String.format("Could not determine domain type from %s!", source));
		}

		if (source != actualType) {
			return detectDomainType(actualType);
		}

		if (source instanceof Iterable) {
			return source;
		}

		return detectDomainType(source.getRequiredComponentType());
	}

}
