/*
 * Copyright 2013-2023 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;

/**
 * {@link HandlerMethodArgumentResolver} to automatically create {@link OffsetScrollPosition} instances from request parameters.
 *
 * @since 3.2
 * @author Yanming Zhou
 */
public class OffsetScrollPositionHandlerMethodArgumentResolver extends OffsetScrollPositionHandlerMethodArgumentResolverSupport
		implements OffsetScrollPositionArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return OffsetScrollPosition.class.equals(parameter.getParameterType());
	}

	@Override
	public OffsetScrollPosition resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		String[] offsetParameter = webRequest.getParameterValues(getOffsetParameter(parameter));
		return parseParameterIntoOffsetScrollPosition(offsetParameter != null ? Arrays.asList(offsetParameter) : null);
	}
}
