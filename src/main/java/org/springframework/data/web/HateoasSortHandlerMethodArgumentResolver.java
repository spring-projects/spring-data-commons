/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extension of {@link SortHandlerMethodArgumentResolver} that also supports enhancing URIs using Spring HATEOAS
 * support.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 */
@SuppressWarnings("null")
public class HateoasSortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolver
		implements UriComponentsContributor {

	/**
	 * Returns the template variables for the sort parameter.
	 *
	 * @param parameter must not be {@literal null}.
	 * @return
	 * @since 1.7
	 */
	public TemplateVariables getSortTemplateVariables(MethodParameter parameter, UriComponents template) {

		String sortParameter = getSortParameter(parameter);
		MultiValueMap<String, String> queryParameters = template.getQueryParams();
		boolean append = !queryParameters.isEmpty();

		if (queryParameters.containsKey(sortParameter)) {
			return TemplateVariables.NONE;
		}

		String description = String.format("pagination.%s.description", sortParameter);
		VariableType type = append ? REQUEST_PARAM_CONTINUED : REQUEST_PARAM;
		return new TemplateVariables(new TemplateVariable(sortParameter, type, description));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.hateoas.mvc.UriComponentsContributor#enhance(org.springframework.web.util.UriComponentsBuilder, org.springframework.core.MethodParameter, java.lang.Object)
	 */
	@Override
	public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {

		if (!(value instanceof Sort)) {
			return;
		}

		Sort sort = (Sort) value;
		String sortParameter = getSortParameter(parameter);

		builder.replaceQueryParam(sortParameter);

		for (String expression : foldIntoExpressions(sort)) {
			builder.queryParam(sortParameter, expression);
		}
	}
}
