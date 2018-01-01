/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.web;

import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MethodLinkBuilderFactory;
import org.springframework.hateoas.core.MethodParameters;
import org.springframework.hateoas.mvc.ControllerLinkBuilderFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link PagedResourcesAssembler} into Spring MVC
 * controller methods.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Christoph Strobl
 */
public class PagedResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(PagedResourcesAssemblerArgumentResolver.class);

	private static final String SUPERFLOUS_QUALIFIER = "Found qualified {} parameter, but a unique unqualified {} parameter. Using that one, but you might wanna check your controller method configuration!";
	private static final String PARAMETER_AMBIGUITY = "Discovered muliple parameters of type Pageable but no qualifier annotations to disambiguate!";

	private final HateoasPageableHandlerMethodArgumentResolver resolver;
	private final MethodLinkBuilderFactory<?> linkBuilderFactory;

	/**
	 * Creates a new {@link PagedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver} and {@link MethodLinkBuilderFactory}.
	 *
	 * @param resolver can be {@literal null}.
	 * @param linkBuilderFactory can be {@literal null}, will be defaulted to a {@link ControllerLinkBuilderFactory}.
	 */
	public PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable MethodLinkBuilderFactory<?> linkBuilderFactory) {

		this.resolver = resolver;
		this.linkBuilderFactory = linkBuilderFactory == null ? new ControllerLinkBuilderFactory() : linkBuilderFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PagedResourcesAssembler.class.equals(parameter.getParameterType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
	 */
	@Nonnull
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		UriComponents fromUriString = resolveBaseUri(parameter);
		MethodParameter pageableParameter = findMatchingPageableParameter(parameter);

		if (pageableParameter != null) {
			return new MethodParameterAwarePagedResourcesAssembler<>(pageableParameter, resolver, fromUriString);
		} else {
			return new PagedResourcesAssembler<>(resolver, fromUriString);
		}
	}

	/**
	 * Eagerly resolve a base URI for the given {@link MethodParameter} to be handed to the assembler.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return the {@link UriComponents} representing the base URI, or {@literal null} if it can't be resolved eagerly.
	 */
	@Nullable
	private UriComponents resolveBaseUri(MethodParameter parameter) {

		try {
			Link linkToMethod = linkBuilderFactory.linkTo(parameter.getDeclaringClass(), parameter.getMethod()).withSelfRel();
			return UriComponentsBuilder.fromUriString(linkToMethod.getHref()).build();
		} catch (IllegalArgumentException o_O) {
			return null;
		}
	}

	/**
	 * Returns finds the {@link MethodParameter} for a {@link Pageable} instance matching the given
	 * {@link MethodParameter} requesting a {@link PagedResourcesAssembler}.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private static MethodParameter findMatchingPageableParameter(MethodParameter parameter) {

		MethodParameters parameters = new MethodParameters(parameter.getMethod());
		List<MethodParameter> pageableParameters = parameters.getParametersOfType(Pageable.class);
		Qualifier assemblerQualifier = parameter.getParameterAnnotation(Qualifier.class);

		if (pageableParameters.isEmpty()) {
			return null;
		}

		if (pageableParameters.size() == 1) {

			MethodParameter pageableParameter = pageableParameters.get(0);
			MethodParameter matchingParameter = returnIfQualifiersMatch(pageableParameter, assemblerQualifier);

			if (matchingParameter == null) {
				LOGGER.info(SUPERFLOUS_QUALIFIER, PagedResourcesAssembler.class.getSimpleName(), Pageable.class.getName());
			}

			return pageableParameter;
		}

		if (assemblerQualifier == null) {
			throw new IllegalStateException(PARAMETER_AMBIGUITY);
		}

		for (MethodParameter pageableParameter : pageableParameters) {

			MethodParameter matchingParameter = returnIfQualifiersMatch(pageableParameter, assemblerQualifier);

			if (matchingParameter != null) {
				return matchingParameter;
			}
		}

		throw new IllegalStateException(PARAMETER_AMBIGUITY);
	}

	@Nullable
	private static MethodParameter returnIfQualifiersMatch(MethodParameter pageableParameter,
			@Nullable Qualifier assemblerQualifier) {

		if (assemblerQualifier == null) {
			return pageableParameter;
		}

		Qualifier pageableParameterQualifier = pageableParameter.getParameterAnnotation(Qualifier.class);

		if (pageableParameterQualifier == null) {
			return null;
		}

		return pageableParameterQualifier.value().equals(assemblerQualifier.value()) ? pageableParameter : null;
	}
}
