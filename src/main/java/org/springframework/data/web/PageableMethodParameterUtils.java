/*
 * Copyright 2023 the original author or authors.
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
import org.springframework.lang.Nullable;

/**
 * Utility methods to obtain a {@link MethodParameter} of type {@link Pageable} with the same {@link Qualifier}.
 *
 * @author Oliver Drotbohm
 * @since 3.1
 * @soundtrack The Intersphere - Wanderer (https://www.youtube.com/watch?v=Sp_VyFBbDPA)
 */
class PageableMethodParameterUtils {

	private static final Log logger = LogFactory.getLog(PageableMethodParameterUtils.class);

	private static final String SUPERFLOUS_QUALIFIER = "Found qualified %s parameter, but a unique unqualified %s parameter; Using that one, but you might want to check your controller method configuration";
	private static final String PARAMETER_AMBIGUITY = "Discovered multiple parameters of type Pageable but no qualifier annotations to disambiguate";

	/**
	 * Returns finds the {@link MethodParameter} for a {@link Pageable} instance matching the given
	 * {@link MethodParameter} requesting a {@link PagedResourcesAssembler}.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@Nullable
	static MethodParameter findMatchingPageableParameter(MethodParameter parameter) {

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

		return pageableParameterQualifier != null
				? pageableParameterQualifier.value().equals(assemblerQualifier.value()) ? pageableParameter : null
				: null;
	}
}
