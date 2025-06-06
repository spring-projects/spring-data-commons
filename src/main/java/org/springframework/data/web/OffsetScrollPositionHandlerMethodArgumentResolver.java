/*
 * Copyright 2023-2025 the original author or authors.
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

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link HandlerMethodArgumentResolver} to automatically create {@link OffsetScrollPosition} instances from request
 * parameters.
 *
 * @author Yanming Zhou
 * @since 3.2
 */
public class OffsetScrollPositionHandlerMethodArgumentResolver extends OffsetScrollPositionHandlerMethodArgumentResolverSupport
		implements OffsetScrollPositionArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return OffsetScrollPosition.class.equals(parameter.nestedIfOptional().getNestedParameterType());
	}

	@Override
	public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		String[] offsetParameter = webRequest.getParameterValues(getOffsetParameter(parameter.nestedIfOptional()));
		return adaptArgumentIfNecessary(
				parseParameterIntoOffsetScrollPosition(offsetParameter != null ? Arrays.asList(offsetParameter) : null),
				parameter);
	}
}
