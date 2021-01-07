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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.web.PageableDefaultUnitTests.*;
import static org.springframework.data.web.PageableHandlerMethodArgumentResolver.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;

/**
 * Unit tests for {@link ReactivePageableHandlerMethodArgumentResolver}.
 *
 * @author Mark Paluch
 */
class ReactivePageableHandlerMethodArgumentResolverUnitTests {

	MethodParameter supportedMethodParameter;

	@BeforeEach
	void setUp() throws Exception {
		this.supportedMethodParameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
	}

	@Test // DATACMNS-1211
	void preventsPageSizeFromExceedingMayValueIfConfigured() {

		// Read side
		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=0&size=200").build();

		assertSupportedAndResult(supportedMethodParameter, PageRequest.of(0, 100), request);
	}

	@Test // DATACMNS-1211
	void rejectsEmptyPageParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactivePageableHandlerMethodArgumentResolver().setPageParameterName(""));
	}

	@Test // DATACMNS-1211
	void rejectsNullPageParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactivePageableHandlerMethodArgumentResolver().setPageParameterName(null));
	}

	@Test // DATACMNS-1211
	void rejectsEmptySizeParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactivePageableHandlerMethodArgumentResolver().setSizeParameterName(""));
	}

	@Test // DATACMNS-1211
	void rejectsNullSizeParameterName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactivePageableHandlerMethodArgumentResolver().setSizeParameterName(null));
	}

	@Test // DATACMNS-1211
	void qualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("validQualifier", Pageable.class), 0);

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?foo_page=2&foo_size=10").build();

		assertSupportedAndResult(parameter, PageRequest.of(2, 10), request);
	}

	@Test // DATACMNS-1211
	void usesDefaultPageSizeIfRequestPageSizeIsLessThanOne() {

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=0&size=0").build();

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-1211
	void rejectsInvalidCustomDefaultForPageSize() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("invalidDefaultPageSize", Pageable.class),
				0);

		assertThatIllegalStateException().isThrownBy(() -> assertSupportedAndResult(parameter, DEFAULT_PAGE_REQUEST))
				.withMessageContaining("invalidDefaultPageSize");
	}

	@Test // DATACMNS-1211
	void fallsBackToFirstPageIfNegativePageNumberIsGiven() {

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=-1").build();

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-1211
	void pageParamIsNotNumeric() {

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=a").build();

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-1211
	void sizeParamIsNotNumeric() {

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?size=a").build();

		assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, request);
	}

	@Test // DATACMNS-1211
	void returnsNullIfFallbackIsUnpagedAndNoParametersGiven() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		assertSupportedAndResult(supportedMethodParameter, Pageable.unpaged(), TestUtils.getWebfluxRequest(), resolver);
	}

	@Test // DATACMNS-1211
	void returnsFallbackIfOnlyPageIsGiven() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=20").build();

		assertThat(resolve(resolver, request)).isEqualTo(Pageable.unpaged());
	}

	@Test // DATACMNS-1211
	void returnsFallbackIfFallbackIsUnpagedAndOnlySizeIsGiven() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?size=10").build();

		assertThat(resolve(resolver, request)).isEqualTo(Pageable.unpaged());
	}

	@Test // DATACMNS-1211
	void considersOneIndexedParametersSetting() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setOneIndexedParameters(true);

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=1").build();

		assertThat(resolve(resolver, request).getPageNumber()).isEqualTo(0);
	}

	@Test // DATACMNS-1211
	void usesNullSortIfNoDefaultIsConfiguredAndPageAndSizeAreGiven() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=0&size=10").build();

		Pageable result = resolve(resolver, request);

		assertThat(result.getPageNumber()).isEqualTo(0);
		assertThat(result.getPageSize()).isEqualTo(10);
		assertThat(result.getSort().isSorted()).isFalse();
	}

	@Test // DATACMNS-1211
	void oneIndexedParametersDefaultsIndexOutOfRange() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setOneIndexedParameters(true);

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?page=0").build();

		assertThat(resolve(resolver, request).getPageNumber()).isEqualTo(0);
	}

	@Test // DATACMNS-1211
	void returnsCorrectPageSizeForOneIndexParameters() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setOneIndexedParameters(true);

		MockServerHttpRequest request = MockServerHttpRequest.get("foo?size=10").build();

		assertThat(resolve(resolver, request).getPageSize()).isEqualTo(10);
	}

	@Test // DATACMNS-1211
	void detectsFallbackPageableIfNullOneIsConfigured() {

		ReactivePageableHandlerMethodArgumentResolver resolver = getReactiveResolver();
		resolver.setFallbackPageable(Pageable.unpaged());

		assertThat(resolver.isFallbackPageable(null)).isFalse();
		assertThat(resolver.isFallbackPageable(PageRequest.of(0, 10))).isFalse();
	}

	private static ReactivePageableHandlerMethodArgumentResolver getReactiveResolver() {

		ReactivePageableHandlerMethodArgumentResolver resolver = new ReactivePageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	private static void assertSupportedAndResult(MethodParameter parameter, Pageable pageable) {
		assertSupportedAndResult(parameter, pageable, TestUtils.getWebfluxRequest());
	}

	private static void assertSupportedAndResult(MethodParameter parameter, Pageable pageable,
			MockServerHttpRequest request) {
		assertSupportedAndResult(parameter, pageable, request, getReactiveResolver());
	}

	private static void assertSupportedAndResult(MethodParameter parameter, Pageable pageable,
			MockServerHttpRequest request,
			SyncHandlerMethodArgumentResolver resolver) {

		assertThat(resolver.supportsParameter(parameter)).isTrue();

		Object value = resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request));
		assertThat(value).isEqualTo(pageable);
	}

	private Pageable resolve(ReactivePageableHandlerMethodArgumentResolver resolver, MockServerHttpRequest request) {
		return resolver.resolveArgumentValue(supportedMethodParameter, null, MockServerWebExchange.from(request));
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
	}
}
