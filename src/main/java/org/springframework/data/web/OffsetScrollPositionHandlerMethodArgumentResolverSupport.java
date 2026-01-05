/*
 * Copyright 2023-present the original author or authors.
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

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Base class providing methods for handler method argument resolvers to create {@link OffsetScrollPosition} instances
 * from request parameters.
 *
 * @author Yanming Zhou
 * @since 3.2
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
	protected String getOffsetParameter(MethodParameter parameter) {

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
	 * @return parsed OffsetScrollPosition or {@literal null} if it cannot be constructed.
	 */
	@Nullable
	OffsetScrollPosition parseParameterIntoOffsetScrollPosition(@Nullable List<String> source) {

		// No parameter or Single empty parameter, e.g "offset="
		if (CollectionUtils.isEmpty(source) || (source.size() == 1 && !StringUtils.hasText(source.get(0)))) {
			return null;
		}

		try {
			long offset = Long.parseLong(source.get(0));
			return ScrollPosition.offset(offset);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * Adapt the given argument against the method parameter, if necessary.
	 *
	 * @param arg the resolved argument.
	 * @param parameter the method parameter descriptor.
	 * @return the adapted argument, or the original resolved argument as-is.
	 */
	@Nullable
	Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {

		if (parameter.getParameterType() == Optional.class) {
			return arg == null ? Optional.empty() : Optional.of(arg);
		}

		return arg;
	}

}
