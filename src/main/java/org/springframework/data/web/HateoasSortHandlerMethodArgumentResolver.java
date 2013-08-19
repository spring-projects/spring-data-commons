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

import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.Assert;
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
public class HateoasSortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolver
		implements UriComponentsContributor {

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

		if (legacyMode) {

			List<String> expressions = legacyFoldExpressions(sort);
			Assert.isTrue(expressions.size() == 2,
					String.format("Expected 2 sort expressions (fields, direction) but got %d!", expressions.size()));
			builder.queryParam(getSortParameter(parameter), expressions.get(0));
			builder.queryParam(getLegacyDirectionParameter(parameter), expressions.get(1));

		} else {

			for (String expression : foldIntoExpressions(sort)) {
				builder.queryParam(getSortParameter(parameter), expression);
			}
		}
	}
}
