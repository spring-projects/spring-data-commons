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
public class HateoasPageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolver implements
		UriComponentsContributor {

	/**
	 * A {@link HateoasPageableHandlerMethodArgumentResolver} preconfigured to the setup of
	 * {@link PageableArgumentResolver}. Use that if you need to stick to the former request parameters an 1-indexed
	 * behavior. This will be removed in the next major version (1.7). So consider migrating to the new way of exposing
	 * request parameters.
	 */
	@Deprecated public static final HateoasPageableHandlerMethodArgumentResolver LEGACY;
	private static final HateoasSortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new HateoasSortHandlerMethodArgumentResolver();

	static {

		HateoasSortHandlerMethodArgumentResolver LEGACY_SORT = new HateoasSortHandlerMethodArgumentResolver();
		LEGACY_SORT.setLegacyMode(true);
		LEGACY_SORT.setSortParameter("page.sort");

		LEGACY = new HateoasPageableHandlerMethodArgumentResolver(LEGACY_SORT);
		LEGACY.setPageParameterName("page.page");
		LEGACY.setSizeParameterName("page.size");
		LEGACY.setFallbackPageable(new PageRequest(1, 10));
		LEGACY.setOneIndexedParameters(true);
	}

	private final HateoasSortHandlerMethodArgumentResolver sortResolver;

	/**
	 * Constructs an instance of this resolver with a default {@link HateoasSortHandlerMethodArgumentResolver}.
	 */
	public HateoasPageableHandlerMethodArgumentResolver() {
		this(null);
	}

	/**
	 * Creates a new {@link HateoasPageableHandlerMethodArgumentResolver} using the given
	 * {@link HateoasSortHandlerMethodArgumentResolver}..
	 * 
	 * @param sortResolver
	 */
	public HateoasPageableHandlerMethodArgumentResolver(HateoasSortHandlerMethodArgumentResolver sortResolver) {

		super(getDefaultedSortResolver(sortResolver));
		this.sortResolver = getDefaultedSortResolver(sortResolver);
	}

	private static HateoasSortHandlerMethodArgumentResolver getDefaultedSortResolver(
			HateoasSortHandlerMethodArgumentResolver sortResolver) {
		return sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
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

		this.sortResolver.enhance(builder, parameter, pageable.getSort());
	}
}
