/*
 * Copyright 2017-2021 the original author or authors.
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

import static org.springframework.data.web.SpringDataAnnotationUtils.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class providing methods for handler method argument resolvers to create paging information from web requests and
 * thus allows injecting {@link Pageable} instances into controller methods. Request properties to be parsed can be
 * configured. Default configuration uses request parameters beginning with
 * {@link #DEFAULT_PAGE_PARAMETER}{@link #DEFAULT_QUALIFIER_DELIMITER}.
 *
 * @since 2.2
 * @see PageableHandlerMethodArgumentResolver
 * @see ReactivePageableHandlerMethodArgumentResolver
 * @author Mark Paluch
 * @author Vedran Pavic
 */
public abstract class PageableHandlerMethodArgumentResolverSupport {

	private static final String INVALID_DEFAULT_PAGE_SIZE = "Invalid default page size configured for method %s! Must not be less than one!";

	private static final String DEFAULT_PAGE_PARAMETER = "page";
	private static final String DEFAULT_SIZE_PARAMETER = "size";
	private static final String DEFAULT_PREFIX = "";
	private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
	private static final int DEFAULT_MAX_PAGE_SIZE = 2000;
	static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 20);

	private Pageable fallbackPageable = DEFAULT_PAGE_REQUEST;
	private String pageParameterName = DEFAULT_PAGE_PARAMETER;
	private String sizeParameterName = DEFAULT_SIZE_PARAMETER;
	private String prefix = DEFAULT_PREFIX;
	private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;
	private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
	private boolean oneIndexedParameters = false;

	/**
	 * Configures the {@link Pageable} to be used as fallback in case no {@link PageableDefault} or
	 * {@link PageableDefault} (the latter only supported in legacy mode) can be found at the method parameter to be
	 * resolved.
	 * <p>
	 * If you set this to {@literal Optional#empty()}, be aware that you controller methods will get {@literal null}
	 * handed into them in case no {@link Pageable} data can be found in the request. Note, that doing so will require you
	 * supply bot the page <em>and</em> the size parameter with the requests as there will be no default for any of the
	 * parameters available.
	 *
	 * @param fallbackPageable the {@link Pageable} to be used as general fallback.
	 */
	public void setFallbackPageable(Pageable fallbackPageable) {

		Assert.notNull(fallbackPageable, "Fallback Pageable must not be null!");

		this.fallbackPageable = fallbackPageable;
	}

	/**
	 * Returns whether the given {@link Pageable} is the fallback one.
	 *
	 * @param pageable can be {@literal null}.
	 * @return
	 */
	public boolean isFallbackPageable(Pageable pageable) {
		return fallbackPageable.equals(pageable);
	}

	/**
	 * Configures the maximum page size to be accepted. This allows to put an upper boundary of the page size to prevent
	 * potential attacks trying to issue an {@link OutOfMemoryError}. Defaults to {@link #DEFAULT_MAX_PAGE_SIZE}.
	 *
	 * @param maxPageSize the maxPageSize to set
	 */
	public void setMaxPageSize(int maxPageSize) {
		this.maxPageSize = maxPageSize;
	}

	/**
	 * Retrieves the maximum page size to be accepted. This allows to put an upper boundary of the page size to prevent
	 * potential attacks trying to issue an {@link OutOfMemoryError}. Defaults to {@link #DEFAULT_MAX_PAGE_SIZE}.
	 *
	 * @return the maximum page size allowed.
	 */
	protected int getMaxPageSize() {
		return this.maxPageSize;
	}

	/**
	 * Configures the parameter name to be used to find the page number in the request. Defaults to {@code page}.
	 *
	 * @param pageParameterName the parameter name to be used, must not be {@literal null} or empty.
	 */
	public void setPageParameterName(String pageParameterName) {

		Assert.hasText(pageParameterName, "Page parameter name must not be null or empty!");
		this.pageParameterName = pageParameterName;
	}

	/**
	 * Retrieves the parameter name to be used to find the page number in the request. Defaults to {@code page}.
	 *
	 * @return the parameter name to be used, never {@literal null} or empty.
	 */
	protected String getPageParameterName() {
		return this.pageParameterName;
	}

	/**
	 * Configures the parameter name to be used to find the page size in the request. Defaults to {@code size}.
	 *
	 * @param sizeParameterName the parameter name to be used, must not be {@literal null} or empty.
	 */
	public void setSizeParameterName(String sizeParameterName) {

		Assert.hasText(sizeParameterName, "Size parameter name must not be null or empty!");
		this.sizeParameterName = sizeParameterName;
	}

	/**
	 * Retrieves the parameter name to be used to find the page size in the request. Defaults to {@code size}.
	 *
	 * @return the parameter name to be used, never {@literal null} or empty.
	 */
	protected String getSizeParameterName() {
		return this.sizeParameterName;
	}

	/**
	 * Configures a general prefix to be prepended to the page number and page size parameters. Useful to namespace the
	 * property names used in case they are clashing with ones used by your application. By default, no prefix is used.
	 *
	 * @param prefix the prefix to be used or {@literal null} to reset to the default.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix == null ? DEFAULT_PREFIX : prefix;
	}

	/**
	 * The delimiter to be used between the qualifier and the actual page number and size properties. Defaults to
	 * {@code _}. So a qualifier of {@code foo} will result in a page number parameter of {@code foo_page}.
	 *
	 * @param qualifierDelimiter the delimiter to be used or {@literal null} to reset to the default.
	 */
	public void setQualifierDelimiter(String qualifierDelimiter) {
		this.qualifierDelimiter = qualifierDelimiter == null ? DEFAULT_QUALIFIER_DELIMITER : qualifierDelimiter;
	}

	/**
	 * Configures whether to expose and assume 1-based page number indexes in the request parameters. Defaults to
	 * {@literal false}, meaning a page number of 0 in the request equals the first page. If this is set to
	 * {@literal true}, a page number of 1 in the request will be considered the first page.
	 *
	 * @param oneIndexedParameters the oneIndexedParameters to set
	 */
	public void setOneIndexedParameters(boolean oneIndexedParameters) {
		this.oneIndexedParameters = oneIndexedParameters;
	}

	/**
	 * Indicates whether to expose and assume 1-based page number indexes in the request parameters. Defaults to
	 * {@literal false}, meaning a page number of 0 in the request equals the first page. If this is set to
	 * {@literal true}, a page number of 1 in the request will be considered the first page.
	 *
	 * @return whether to assume 1-based page number indexes in the request parameters.
	 */
	protected boolean isOneIndexedParameters() {
		return this.oneIndexedParameters;
	}

	protected Pageable getPageable(MethodParameter methodParameter, @Nullable String pageString,
			@Nullable String pageSizeString) {
		assertPageableUniqueness(methodParameter);

		Optional<Pageable> defaultOrFallback = getDefaultFromAnnotationOrFallback(methodParameter).toOptional();

		Optional<Integer> page = parseAndApplyBoundaries(pageString, Integer.MAX_VALUE, true);
		Optional<Integer> pageSize = parseAndApplyBoundaries(pageSizeString, maxPageSize, false);

		if (!(page.isPresent() && pageSize.isPresent()) && !defaultOrFallback.isPresent()) {
			return Pageable.unpaged();
		}

		int p = page
				.orElseGet(() -> defaultOrFallback.map(Pageable::getPageNumber).orElseThrow(IllegalStateException::new));
		int ps = pageSize
				.orElseGet(() -> defaultOrFallback.map(Pageable::getPageSize).orElseThrow(IllegalStateException::new));

		// Limit lower bound
		ps = ps < 1 ? defaultOrFallback.map(Pageable::getPageSize).orElseThrow(IllegalStateException::new) : ps;
		// Limit upper bound
		ps = ps > maxPageSize ? maxPageSize : ps;

		return PageRequest.of(p, ps, defaultOrFallback.map(Pageable::getSort).orElseGet(Sort::unsorted));
	}

	/**
	 * Returns the name of the request parameter to find the {@link Pageable} information in. Inspects the given
	 * {@link MethodParameter} for {@link Qualifier} present and prefixes the given source parameter name with it.
	 *
	 * @param source the basic parameter name.
	 * @param parameter the {@link MethodParameter} potentially qualified.
	 * @return the name of the request parameter.
	 */
	protected String getParameterNameToUse(String source, @Nullable MethodParameter parameter) {

		StringBuilder builder = new StringBuilder(prefix);

		String value = SpringDataAnnotationUtils.getQualifier(parameter);

		if (StringUtils.hasLength(value)) {
			builder.append(value);
			builder.append(qualifierDelimiter);
		}

		return builder.append(source).toString();
	}

	private Pageable getDefaultFromAnnotationOrFallback(MethodParameter methodParameter) {

		PageableDefault defaults = methodParameter.getParameterAnnotation(PageableDefault.class);

		if (defaults != null) {
			return getDefaultPageRequestFrom(methodParameter, defaults);
		}

		return fallbackPageable;
	}

	private static Pageable getDefaultPageRequestFrom(MethodParameter parameter, PageableDefault defaults) {

		Integer defaultPageNumber = defaults.page();
		Integer defaultPageSize = getSpecificPropertyOrDefaultFromValue(defaults, "size");

		if (defaultPageSize < 1) {
			Method annotatedMethod = parameter.getMethod();
			throw new IllegalStateException(String.format(INVALID_DEFAULT_PAGE_SIZE, annotatedMethod));
		}

		if (defaults.sort().length == 0) {
			return PageRequest.of(defaultPageNumber, defaultPageSize);
		}

		return PageRequest.of(defaultPageNumber, defaultPageSize, defaults.direction(), defaults.sort());
	}

	/**
	 * Tries to parse the given {@link String} into an integer and applies the given boundaries. Will return 0 if the
	 * {@link String} cannot be parsed.
	 *
	 * @param parameter the parameter value.
	 * @param upper the upper bound to be applied.
	 * @param shiftIndex whether to shift the index if {@link #oneIndexedParameters} is set to true.
	 * @return
	 */
	private Optional<Integer> parseAndApplyBoundaries(@Nullable String parameter, int upper, boolean shiftIndex) {

		if (!StringUtils.hasText(parameter)) {
			return Optional.empty();
		}

		try {
			int parsed = Integer.parseInt(parameter) - (oneIndexedParameters && shiftIndex ? 1 : 0);
			return Optional.of(parsed < 0 ? 0 : parsed > upper ? upper : parsed);
		} catch (NumberFormatException e) {
			return Optional.of(0);
		}
	}


}
