/*
 * Copyright 2017-2023 the original author or authors.
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
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Base class providing methods for handler method argument resolvers to create {@link OffsetScrollPosition} instances from request
 * parameters.
 *
 * @since 3.2
 * @author Yanming Zhou
 * @see OffsetScrollPositionHandlerMethodArgumentResolver
 * @see ReactiveOffsetScrollPositionHandlerMethodArgumentResolver
 */
public abstract class OffsetScrollPositionHandlerMethodArgumentResolverSupport {

	private static final String DEFAULT_PARAMETER = "offset";

	private static final String DEFAULT_QUALIFIER_DELIMITER = "_";

	private String offsetParameter = DEFAULT_PARAMETER;

	private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;

	/**
	 * Configure the request parameter to lookup offset information from. Defaults to {@code offset}.
	 *
	 * @param offsetParameter must not be {@literal null} or empty.
	 */
	public void setOffsetParameter(String offsetParameter) {

		Assert.hasText(offsetParameter, "offsetParameter must not be null nor empty");
		this.offsetParameter = offsetParameter;
	}

	/**
	 * Configures the delimiter used to separate the qualifier from the offset parameter. Defaults to {@code _}, so a
	 * qualified offset property would look like {@code qualifier_offset}.
	 *
	 * @param qualifierDelimiter the qualifier delimiter to be used or {@literal null} to reset to the default.
	 */
	public void setQualifierDelimiter(@Nullable String qualifierDelimiter) {
		this.qualifierDelimiter = qualifierDelimiter == null ? DEFAULT_QUALIFIER_DELIMITER : qualifierDelimiter;
	}

	/**
	 * Returns the offset parameter to be looked up from the request. Potentially applies qualifiers to it.
	 *
	 * @param parameter can be {@literal null}.
	 * @return the offset parameter
	 */
	protected String getOffsetParameter(@Nullable MethodParameter parameter) {

		StringBuilder builder = new StringBuilder();

		String value = SpringDataAnnotationUtils.getQualifier(parameter);

		if (StringUtils.hasLength(value)) {
			builder.append(value);
			builder.append(qualifierDelimiter);
		}

		return builder.append(offsetParameter).toString();
	}

	/**
	 * Parses the given source into a {@link OffsetScrollPosition} instance.
	 *
	 * @param source could be {@literal null} or empty.
	 * @return parsed OffsetScrollPosition
	 */
	OffsetScrollPosition parseParameterIntoOffsetScrollPosition(@Nullable List<String> source) {
		// No parameter or Single empty parameter, e.g "offset="
		if (source == null || source.size() == 1 && !StringUtils.hasText(source.get(0))) {
			return ScrollPosition.offset();
		}
		try {
			long offset = Long.parseLong(source.get(0));
			return ScrollPosition.offset(offset);
		} catch (NumberFormatException ex) {
			return ScrollPosition.offset();
		}
	}

}
