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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.web.SortDefaultUnitTests.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Base test class to test supporting of a {@link HandlerMethodArgumentResolver} implementation defaulting
 * {@link Pageable} method parameters. Expects the {@link HandlerMethodArgumentResolver} to be tested returned from
 * {@link #getResolver()} and expects methods to be present in the controller class returned from
 * {@link #getControllerClass()}. For sample usage see {@link PageableHandlerMethodArgumentResolver}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 * @author Nick Williams
 */
public abstract class PageableDefaultUnitTests {

	static final int PAGE_SIZE = 47;
	static final int PAGE_NUMBER = 23;

	static final AbstractPageRequest REFERENCE_WITHOUT_SORT = PageRequest.of(PAGE_NUMBER, PAGE_SIZE);
	static final AbstractPageRequest REFERENCE_WITH_SORT = PageRequest.of(PAGE_NUMBER, PAGE_SIZE, SORT);
	static final AbstractPageRequest REFERENCE_WITH_SORT_FIELDS = PageRequest.of(PAGE_NUMBER, PAGE_SIZE,
			Sort.by(SORT_FIELDS));

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test
	public void supportsPageable() {
		assertThat(getResolver().supportsParameter(getParameterOfMethod("supportedMethod"))).isTrue();
	}

	@Test
	public void doesNotSupportNonPageable() {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		assertThat(getResolver().supportsParameter(parameter)).isFalse();
	}

	@Test
	public void returnsDefaultIfNoRequestParametersAndNoDefault() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("supportedMethod"), (Pageable) ReflectionTestUtils
				.invokeMethod(ReflectionTestUtils.getField(getResolver(), "fallbackPageable"), "get"));
	}

	@Test
	public void simpleDefault() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("simpleDefault"), REFERENCE_WITHOUT_SORT);
	}

	@Test
	public void simpleDefaultWithSort() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithSort"), REFERENCE_WITH_SORT_FIELDS);
	}

	@Test
	public void simpleDefaultWithSortAndDirection() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithSortAndDirection"), REFERENCE_WITH_SORT);
	}

	@Test
	public void simpleDefaultWithExternalSort() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithExternalSort"), REFERENCE_WITH_SORT);
	}

	@Test
	public void simpleDefaultWithContaineredExternalSort() throws Exception {
		assertSupportedAndResult(getParameterOfMethod("simpleDefaultWithContaineredExternalSort"), REFERENCE_WITH_SORT);
	}

	@Test
	public void rejectsInvalidQulifiers() throws Exception {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "invalidQualifiers",
				Pageable.class, Pageable.class);

		HandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		exception.expect(IllegalStateException.class);
		exception.expectMessage("unique");

		resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null);
	}

	@Test
	public void rejectsNoQualifiers() throws Exception {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "noQualifiers", Pageable.class,
				Pageable.class);

		HandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		exception.expect(IllegalStateException.class);
		exception.expectMessage("Ambiguous");

		resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null);
	}

	protected void assertSupportedAndResult(MethodParameter parameter, Pageable pageable) throws Exception {
		assertSupportedAndResult(parameter, pageable, TestUtils.getWebRequest());
	}

	protected void assertSupportedAndResult(MethodParameter parameter, Pageable pageable, HttpServletRequest request)
			throws Exception {
		assertSupportedAndResult(parameter, pageable, new ServletWebRequest(request));
	}

	protected void assertSupportedAndResult(MethodParameter parameter, Pageable pageable, NativeWebRequest request)
			throws Exception {

		assertSupportedAndResult(parameter, pageable, request, getResolver());
	}

	protected void assertSupportedAndResult(MethodParameter parameter, Pageable pageable, NativeWebRequest request,
			HandlerMethodArgumentResolver resolver) throws Exception {

		assertThat(resolver.supportsParameter(parameter)).isTrue();
		assertThat(resolver.resolveArgument(parameter, null, request, null)).isEqualTo(pageable);
	}

	protected abstract PageableHandlerMethodArgumentResolver getResolver();

	protected abstract Class<?> getControllerClass();

	protected MethodParameter getParameterOfMethod(String name) {
		return getParameterOfMethod(getControllerClass(), name);
	}

	private static MethodParameter getParameterOfMethod(Class<?> controller, String name) {
		return TestUtils.getParameterOfMethod(controller, name, Pageable.class);
	}
}
