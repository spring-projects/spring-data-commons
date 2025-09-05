/*
 * Copyright 2016-2025 the original author or authors.
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Argument resolver to extract a {@link Pageable} object from a {@link NativeWebRequest} for a particular
 * {@link MethodParameter}. A {@link PageableArgumentResolver} can either resolve {@link Pageable} itself or wrap
 * another {@link PageableArgumentResolver} to post-process {@link Pageable}.
 *
 * @author Mark Paluch
 * @since 1.13
 * @see org.springframework.web.method.support.HandlerMethodArgumentResolver
 */
public interface PageableArgumentResolver extends HandlerMethodArgumentResolver {

	/**
	 * Resolves a {@link Pageable} method parameter into an argument value from a given request.
	 *
	 * @param parameter the method parameter to resolve. This parameter must have previously been passed to
	 *          {@link #supportsParameter} which must have returned {@code true}.
	 * @param mavContainer the ModelAndViewContainer for the current request
	 * @param webRequest the current request
	 * @param binderFactory a factory for creating {@link WebDataBinder} instances
	 * @return the resolved argument value.
	 */
	@NonNull
	@Override
	Pageable resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory);
}
