/*
 * Copyright 2022-2023 the original author or authors.
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
 * @author Oliver Drotbohm
 * @since 3.1
 */
public class SlicedResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

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

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return SlicedResourcesAssembler.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		return new SlicedResourcesAssembler<>(resolver, null) //
				.withParameter(PageableMethodParameterUtils.findMatchingPageableParameter(parameter));
	}
}
