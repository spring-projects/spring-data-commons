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
import org.springframework.lang.NonNull;
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
 * @author Johannes Englmeier
 */
public class PagedResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

	private final HateoasPageableHandlerMethodArgumentResolver resolver;

	/**
	 * Creates a new {@link PagedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver}.
	 *
	 * @param resolver can be {@literal null}.
	 */
	public PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return PagedResourcesAssembler.class.equals(parameter.getParameterType());
	}

	@NonNull
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {

		return new PagedResourcesAssembler<>(resolver, null)
				.withParameter(PageableMethodParameterUtils.findMatchingPageableParameter(parameter));
	}
}
