/*
 * Copyright 2013-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.web.PageableHandlerMethodArgumentResolver.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link PageableHandlerMethodArgumentResolver}. Pulls in defaulting tests from
 * {@link PageableDefaultUnitTests}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 * @author Vedran Pavic
 */
class PageableHandlerMethodArgumentResolverUnitTests extends PageableDefaultUnitTests {

	MethodParameter supportedMethodParameter;

	@BeforeEach
	void setUp() throws Exception {
		this.supportedMethodParameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
	}

	@Test // DATACMNS-335
	void preventsPageSizeFromExceedingMayValueIfConfigured() throws Exception {

		// Read side
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "200");

		assertSupportedAndResult(supportedMethodParameter, PageRequest.of(0, 100), request);
	}

	@Test
	void rejectsEmptyPageParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PageableHandlerMethodArgumentResolver().setPageParameterName(""));
	}

	@Test
	void rejectsNullPageParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PageableHandlerMethodArgumentResolver().setPageParameterName(null));
	}

	@Test
	void rejectsEmptySizeParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PageableHandlerMethodArgumentResolver().setSizeParameterName(""));
	}

	@Test
	void rejectsNullSizeParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PageableHandlerMethodArgumentResolver().setSizeParameterName(null));
	}

	@Test
	void qualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("validQualifier", Pageable.class), 0);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo_page", "2");
		request.addParameter("foo_size", "10");

		assertSupportedAndResult(parameter, PageRequest.of(2, 10), request);
	}

	@Test // DATACMNS-377
	void usesDefaultPageSizeIfRequestPageSizeIsLessThanOne() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "0");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-377
	void rejectsInvalidCustomDefaultForPageSize() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("invalidDefaultPageSize", Pageable.class),
				0);

		assertThatIllegalStateException().isThrownBy(() -> assertSupportedAndResult(parameter, DEFAULT_PAGE_REQUEST)) //
				.withMessageContaining("invalidDefaultPageSize");
	}

	@Test // DATACMNS-408
	void fallsBackToFirstPageIfNegativePageNumberIsGiven() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "-1");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-408
	void pageParamIsNotNumeric() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "a");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-408
	void sizeParamIsNotNumeric() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "a");

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-477
	void returnsNullIfFallbackIsUnpagedAndNoParametersGiven() throws Exception {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		assertSupportedAndResult(supportedMethodParameter, Pageable.unpaged(),
				new ServletWebRequest(new MockHttpServletRequest()), resolver);
	}

	@Test // DATACMNS-477
	void returnsFallbackIfOnlyPageIsGiven() throws Exception {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "20");

		assertThat(resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null))
				.isEqualTo(Pageable.unpaged());
	}

	@Test // DATACMNS-477
	void returnsFallbackIfFallbackIsUnpagedAndOnlySizeIsGiven() throws Exception {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "10");

		assertThat(resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null))
				.isEqualTo(Pageable.unpaged());
	}

	@Test // DATACMNS-563
	void considersOneIndexedParametersSetting() {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setOneIndexedParameters(true);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "1");

		assertThat(
				resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null).getPageNumber())
						.isEqualTo(0);
	}

	@Test // DATACMNS-640
	void usesNullSortIfNoDefaultIsConfiguredAndPageAndSizeAreGiven() {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "10");

		Pageable result = resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null);

		assertThat(result.getPageNumber()).isEqualTo(0);
		assertThat(result.getPageSize()).isEqualTo(10);
		assertThat(result.getSort().isSorted()).isFalse();
	}

	@Test // DATACMNS-692
	void oneIndexedParametersDefaultsIndexOutOfRange() {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setOneIndexedParameters(true);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");

		Pageable result = resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null);

		assertThat(result.getPageNumber()).isEqualTo(0);
	}

	@Test // DATACMNS-761
	void returnsCorrectPageSizeForOneIndexParameters() {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setOneIndexedParameters(true);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("size", "10");

		Pageable result = resolver.resolveArgument(supportedMethodParameter, null, new ServletWebRequest(request), null);

		assertThat(result.getPageSize()).isEqualTo(10);
	}

	@Test // DATACMNS-929
	void detectsFallbackPageableIfNullOneIsConfigured() {

		PageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		assertThat(resolver.isFallbackPageable(null)).isFalse();
		assertThat(resolver.isFallbackPageable(PageRequest.of(0, 10))).isFalse();
	}

	@Test // DATACMNS-1827
	void emptyQualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("emptyQualifier", Pageable.class), 0);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "2");
		request.addParameter("size", "10");

		assertSupportedAndResult(parameter, PageRequest.of(2, 10), request);
	}

	@Test // DATACMNS-1827
	void mergedQualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("mergedQualifier", Pageable.class), 0);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("merged_page", "2");
		request.addParameter("merged_size", "10");

		assertSupportedAndResult(parameter, PageRequest.of(2, 10), request);
	}

	@Override
	protected PageableHandlerMethodArgumentResolver getResolver() {
		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	@Override
	protected Class<?> getControllerClass() {
		return Sample.class;
	}

	interface Sample {

		void supportedMethod(Pageable pageable);

		void unsupportedMethod(String string);

		void invalidDefaultPageSize(@PageableDefault(size = 0) Pageable pageable);

		void simpleDefault(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) Pageable pageable);

		void simpleDefaultWithSort(
				@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER, sort = { "firstname", "lastname" }) Pageable pageable);

		void simpleDefaultWithSortAndDirection(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER,
				sort = { "firstname", "lastname" }, direction = Direction.DESC) Pageable pageable);

		void simpleDefaultWithExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) //
		@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Pageable pageable);

		void simpleDefaultWithContaineredExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) //
		@SortDefaults(@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC)) Pageable pageable);

		void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second);

		void validQualifier(@Qualifier("foo") Pageable pageable);

		void noQualifiers(Pageable first, Pageable second);

		void emptyQualifier(@Qualifier Pageable pageable);

		void mergedQualifier(@TestQualifier Pageable pageable);
	}
}
