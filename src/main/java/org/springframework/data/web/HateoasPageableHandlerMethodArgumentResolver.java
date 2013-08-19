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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extension of {@link PageableHandlerMethodArgumentResolver} that also supports enhancing URIs using Spring HATEOAS
 * support.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 */
@SuppressWarnings("deprecation")
public class HateoasPageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolver
		implements UriComponentsContributor {

	/**
	 * A {@link HateoasPageableHandlerMethodArgumentResolver} preconfigured to the setup of
	 * {@link PageableArgumentResolver}. Use that if you need to stick to the former request parameters an 1-indexed
	 * behavior. This will be removed in the next major version (1.7). So consider migrating to the new way of exposing
	 * request parameters.
	 */
	@Deprecated public static final HateoasPageableHandlerMethodArgumentResolver LEGACY;

	static {
		LEGACY = new HateoasPageableHandlerMethodArgumentResolver();
		LEGACY.setPageParameterName("page.page");
		LEGACY.setSizeParameterName("page.size");
		LEGACY.setFallbackPageable(new PageRequest(1, 10));
		LEGACY.setOneIndexedParameters(true);
		LEGACY.getSortResolver().setLegacyMode(true);
		LEGACY.getSortResolver().setSortParameter("page.sort");
	}

	/**
	 * Constructs an instance of this resolver with a default {@link HateoasSortHandlerMethodArgumentResolver}.
	 */
	public HateoasPageableHandlerMethodArgumentResolver() {
		super(new HateoasSortHandlerMethodArgumentResolver());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.mvc.UriComponentsContributor#enhance(org.springframework.web.util.UriComponentsBuilder, org.springframework.core.MethodParameter, java.lang.Object)
	 */
	@Override
	public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {

		if (!(value instanceof Pageable)) {
			return;
		}

		Pageable pageable = (Pageable) value;

		String pagePropertyName = getParameterNameToUse(getPageParameterName(), parameter);
		String sizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);

		int pageNumber = pageable.getPageNumber();

		builder.replaceQueryParam(pagePropertyName, isOneIndexedParameters() ? pageNumber + 1 : pageNumber);
		builder.replaceQueryParam(sizePropertyName, pageable.getPageSize() <= getMaxPageSize() ? pageable.getPageSize()
				: getMaxPageSize());

		((HateoasSortHandlerMethodArgumentResolver) getSortResolver()).enhance(builder, parameter, pageable.getSort());
	}

	/**
	 * Configure the {@link HateoasSortHandlerMethodArgumentResolver} to be used with the
	 * {@link HateoasPageableHandlerMethodArgumentResolver}.
	 *
	 * @param sortResolver the {@link HateoasSortHandlerMethodArgumentResolver} to be used or {@literal null} to reset
	 *                     it to the default one.
	 * @throws IllegalArgumentException if the argument does not extend {@link HateoasSortHandlerMethodArgumentResolver}.
	 */
	@Override
	public void setSortResolver(SortHandlerMethodArgumentResolver sortResolver) {
		if (sortResolver == null) {
			sortResolver = new HateoasSortHandlerMethodArgumentResolver();
		} else if(!(sortResolver instanceof HateoasSortHandlerMethodArgumentResolver)) {
			throw new IllegalArgumentException("Sort resolver not instance of HateoasSortHandlerMethodArgumentResolver");
		}

		super.setSortResolver(sortResolver);
	}
}
