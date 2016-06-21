/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.springframework.hateoas.TemplateVariable.VariableType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extension of {@link PageableHandlerMethodArgumentResolver} that also supports enhancing URIs using Spring HATEOAS
 * support.
 * 
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class HateoasPageableHandlerMethodArgumentResolver extends PageableHandlerMethodArgumentResolver implements
		UriComponentsContributor {

	private static final HateoasSortHandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new HateoasSortHandlerMethodArgumentResolver();

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

	/**
	 * Returns the template variable for the pagination parameters.
	 * 
	 * @param parameter can be {@literal null}.
	 * @return
	 * @since 1.7
	 */
	public TemplateVariables getPaginationTemplateVariables(MethodParameter parameter, UriComponents template) {

		String pagePropertyName = getParameterNameToUse(getPageParameterName(), parameter);
		String sizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);

		List<TemplateVariable> names = new ArrayList<>();
		MultiValueMap<String, String> queryParameters = template.getQueryParams();
		boolean append = !queryParameters.isEmpty();

		for (String propertyName : Arrays.asList(pagePropertyName, sizePropertyName)) {

			if (!queryParameters.containsKey(propertyName)) {

				VariableType type = append ? REQUEST_PARAM_CONTINUED : REQUEST_PARAM;
				String description = String.format("pagination.%s.description", propertyName);
				names.add(new TemplateVariable(propertyName, type, description));
			}
		}

		TemplateVariables pagingVariables = new TemplateVariables(names);
		return pagingVariables.concat(sortResolver.getSortTemplateVariables(parameter, template));
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

	private static HateoasSortHandlerMethodArgumentResolver getDefaultedSortResolver(
			HateoasSortHandlerMethodArgumentResolver sortResolver) {
		return sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
	}
}
