/*
 * Copyright 2013-2021 the original author or authors.
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
package org.springframework.data.web;

import java.lang.reflect.Method;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.server.MethodLinkBuilderFactory;
import org.springframework.hateoas.server.core.MethodParameters;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilderFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

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

	private static final Log logger = LogFactory.getLog(PagedResourcesAssemblerArgumentResolver.class);

	private static final String SUPERFLOUS_QUALIFIER = "Found qualified %s parameter, but a unique unqualified %s parameter. Using that one, but you might want to check your controller method configuration!";
	private static final String PARAMETER_AMBIGUITY = "Discovered multiple parameters of type Pageable but no qualifier annotations to disambiguate!";

	private final HateoasPageableHandlerMethodArgumentResolver resolver;

	/**
	 * Creates a new {@link PagedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver} and {@link MethodLinkBuilderFactory}.
	 *
	 * @param resolver can be {@literal null}.
	 * @param linkBuilderFactory can be {@literal null}, will be defaulted to a {@link WebMvcLinkBuilderFactory}.
	 * @deprecated since 2.5, 2.4.4, 2.3.7, to be removed in 3.0
	 * @use {@link #PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver)} instead.
	 */
	@Deprecated
	public PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver,
			@Nullable MethodLinkBuilderFactory<?> linkBuilderFactory) {
		this(resolver);
	}

	/**
	 * Creates a new {@link PagedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver}.
	 *
	 * @param resolver can be {@literal null}.
	 */
	public PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver) {
		this.resolver = resolver;
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

		MethodParameter pageableParameter = findMatchingPageableParameter(parameter);

		if (pageableParameter != null) {
			return new MethodParameterAwarePagedResourcesAssembler<>(pageableParameter, resolver, null);
		} else {
			return new PagedResourcesAssembler<>(resolver, null);
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

		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException(String.format("Could not obtain method from parameter %s!", parameter));
		}

		MethodParameters parameters = MethodParameters.of(method);
		List<MethodParameter> pageableParameters = parameters.getParametersOfType(Pageable.class);
		Qualifier assemblerQualifier = parameter.getParameterAnnotation(Qualifier.class);

		if (pageableParameters.isEmpty()) {
			return null;
		}

		if (pageableParameters.size() == 1) {

			MethodParameter pageableParameter = pageableParameters.get(0);
			MethodParameter matchingParameter = returnIfQualifiersMatch(pageableParameter, assemblerQualifier);

			if (matchingParameter == null) {
				logger.info(LogMessage.format(SUPERFLOUS_QUALIFIER, PagedResourcesAssembler.class.getSimpleName(),
						Pageable.class.getName()));
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
