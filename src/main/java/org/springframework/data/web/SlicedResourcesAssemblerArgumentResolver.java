/*
 * Copyright 2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.server.core.MethodParameters;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link SlicedResourcesAssembler} into Spring MVC
 * controller methods.
 *
 * @author Michael Schout
 */
public class SlicedResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {
	private static final Log logger = LogFactory.getLog(SlicedResourcesAssemblerArgumentResolver.class);

	private static final String SUPERFLOUS_QUALIFIER = "Found qualified %s parameter, but a unique unqualified %s parameter; Using that one, but you might want to check your controller method configuration";
	private static final String PARAMETER_AMBIGUITY = "Discovered multiple parameters of type Pageable but no qualifier annotations to disambiguate";

	private final HateoasPageableHandlerMethodArgumentResolver resolver;

	/**
	 * Creates a new {@link SlicedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver}.
	 *
	 * @param resolver can be {@literal null}.
	 */
	public SlicedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver) {
		this.resolver = resolver;
	}

	/**
	 * Returns finds the {@link MethodParameter} for a {@link Pageable} instance matching the
	 * given {@link MethodParameter} requesting a {@link SlicedResourcesAssembler}.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	@Nullable
	private static MethodParameter findMatchingPageableParameter(MethodParameter parameter) {
		Method method = parameter.getMethod();

		if (method == null) {
			throw new IllegalArgumentException(String.format("Could not obtain method from parameter %s", parameter));
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
				logger.info(LogMessage.format(SUPERFLOUS_QUALIFIER, SlicedResourcesAssembler.class.getSimpleName(),
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

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SlicedResourcesAssembler.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		MethodParameter pageableParameter = findMatchingPageableParameter(parameter);

		if (pageableParameter != null) {
			return new MethodParameterAwareSlicedResourcesAssembler<>(pageableParameter, resolver, null);
		}
		else {
			return new SlicedResourcesAssembler<>(resolver, null);
		}
	}
}
