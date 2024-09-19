/*
 * Copyright 2013-2024 the original author or authors.
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
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

/**
 * Optional wrapper resolver for {@link PageableHandlerMethodArgumentResolver}.
 *
 * @author Yanming Zhou
 */
public class OptionalPageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver;

	/**
	 * Constructs an instance of this resolver with the specified {@link PageableHandlerMethodArgumentResolver}.
	 *
	 * @param pageableHandlerMethodArgumentResolver the pageable resolver to use
	 */
	public OptionalPageableHandlerMethodArgumentResolver(PageableHandlerMethodArgumentResolver pageableHandlerMethodArgumentResolver) {
		this.pageableHandlerMethodArgumentResolver = pageableHandlerMethodArgumentResolver;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return Optional.class == parameter.getParameterType() && Pageable.class == parameter.nested().getParameterType();
	}

	@Override
	public Optional<Pageable> resolveArgument(MethodParameter methodParameter, @Nullable ModelAndViewContainer mavContainer,
											  NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
		return Optional.of(pageableHandlerMethodArgumentResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory));
	}
}
