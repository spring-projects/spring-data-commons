/*
 * Copyright 2013-present the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriUtils;

/**
 * {@link org.springframework.web.method.support.HandlerMethodArgumentResolver} to automatically create {@link Sort}
 * instances from request parameters or {@link SortDefault} annotations.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 1.6
 */
public class SortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolverSupport
		implements SortArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Sort.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public Sort resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		String[] directionParameter = webRequest.getParameterValues(getSortParameter(parameter));

		// No parameter
		if (directionParameter == null) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		// Single empty parameter, e.g "sort="
		if (directionParameter.length == 1 && !StringUtils.hasText(directionParameter[0])) {
			return getDefaultFromAnnotationOrFallback(parameter);
		}

		var decoded = Arrays.stream(directionParameter)
				.map(it -> UriUtils.decode(it, StandardCharsets.UTF_8))
				.toList();

		return parseParameterIntoSort(decoded, getPropertyDelimiter());
	}
}
