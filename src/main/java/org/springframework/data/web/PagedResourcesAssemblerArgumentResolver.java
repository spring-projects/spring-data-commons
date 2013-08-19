/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import org.springframework.core.MethodParameter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MethodLinkBuilderFactory;
import org.springframework.hateoas.mvc.ControllerLinkBuilderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link PagedResourcesAssembler} into Spring MVC
 * controller methods.
 * 
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PagedResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

	private final HateoasPageableHandlerMethodArgumentResolver resolver;
	private final MethodLinkBuilderFactory<?> linkBuilderFactory;

	/**
	 * Creates a new {@link PagedResourcesAssemblerArgumentResolver} using the given
	 * {@link PageableHandlerMethodArgumentResolver} and {@link MethodLinkBuilderFactory}.
	 * 
	 * @param resolver can be {@literal null}.
	 * @param linkBuilderFactory can be {@literal null}, will be defaulted to a {@link ControllerLinkBuilderFactory}.
	 */
	public PagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver resolver,
			MethodLinkBuilderFactory<?> linkBuilderFactory) {

		this.resolver = resolver;
		this.linkBuilderFactory = linkBuilderFactory == null ? new ControllerLinkBuilderFactory() : linkBuilderFactory;
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
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Link linkToMethod = linkBuilderFactory.linkTo(parameter.getMethod(), new Object[0]).withSelfRel();
		UriComponents fromUriString = UriComponentsBuilder.fromUriString(linkToMethod.getHref()).build();

		return new PagedResourcesAssembler<Object>(resolver, fromUriString);
	}
}
