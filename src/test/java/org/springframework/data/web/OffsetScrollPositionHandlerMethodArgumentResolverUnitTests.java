/*
 * Copyright 2013-2023 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OffsetScrollPositionHandlerMethodArgumentResolver}.
 *
 * @since 3.2
 * @author Yanming Zhou
 */
class OffsetScrollPositionHandlerMethodArgumentResolverUnitTests {

	static MethodParameter PARAMETER;

	@BeforeAll
	static void setUp() throws Exception {
		PARAMETER = getParameterOfMethod("supportedMethod");
	}

	@Test
	void supportsSortParameter() {

		var resolver = new OffsetScrollPositionHandlerMethodArgumentResolver();

		assertThat(resolver.supportsParameter(PARAMETER)).isTrue();
	}

	@Test
	void fallbackToDefaultOffset() {

		var parameter = TestUtils.getParameterOfMethod(Controller.class, "unsupportedMethod", String.class);
		var position = resolveOffset(new MockHttpServletRequest(), parameter);
		assertThat(position).isEqualTo(ScrollPosition.offset());
	}

	@Test
	void discoversOffsetFromRequest() {

		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, null), PARAMETER, reference);
	}

	@Test
	void discoversOffsetFromRequestWithMultipleParams() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "5");
		request.addParameter("offset", "6");

		assertThat(resolveOffset(request, PARAMETER)).isEqualTo(ScrollPosition.offset(5));
	}

	@Test
	void discoversQualifiedOffsetFromRequest() {

		var parameter = getParameterOfMethod("qualifiedOffset");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, "qual"), parameter, reference);
	}

	@Test
	void returnsDefaultForOffsetParamSetToNothing() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", (String) null);

		assertThat(resolveOffset(request, PARAMETER)).isEqualTo(ScrollPosition.offset());
	}

	@Test
	void returnsDefaultForEmptyOffsetParam() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "");

		assertThat(resolveOffset(request, PARAMETER)).isEqualTo(ScrollPosition.offset());
	}

	@Test
	void returnsDefaultForOffsetParamIsInvalidProperty() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "invalid_number");

		assertThat(resolveOffset(request, PARAMETER)).isEqualTo(ScrollPosition.offset());
	}

	@Test
	void emptyQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("emptyQualifier");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, ""), parameter, reference);
	}

	@Test
	void mergedQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("mergedQualifier");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, "merged"), parameter, reference);
	}

	private static OffsetScrollPosition resolveOffset(HttpServletRequest request, MethodParameter parameter) {

		var resolver = new OffsetScrollPositionHandlerMethodArgumentResolver();
		return resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
	}

	private static void assertSupportedAndResolvedTo(NativeWebRequest request, MethodParameter parameter, OffsetScrollPosition position) {

		var resolver = new OffsetScrollPositionHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		try {
			assertThat(resolver.resolveArgument(parameter, null, request, null)).isEqualTo(position);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	private static MethodParameter getParameterOfMethod(String name) {
		return TestUtils.getParameterOfMethod(Controller.class, name, OffsetScrollPosition.class);
	}

	private static NativeWebRequest getRequestWithOffset(@Nullable OffsetScrollPosition position, @Nullable String qualifier) {

		var request = new MockHttpServletRequest();

		if (position == null) {
			return new ServletWebRequest(request);
		}

		String parameterName = StringUtils.hasLength(qualifier) ? qualifier + "_offset" : "offset";
		request.addParameter(parameterName, String.valueOf(position.getOffset()));

		return new ServletWebRequest(request);
	}

	interface Controller {

		void supportedMethod(OffsetScrollPosition offset);

		void unsupportedMethod(String string);

		void qualifiedOffset(@Qualifier("qual") OffsetScrollPosition offset);

		void emptyQualifier(@Qualifier OffsetScrollPosition offset);

		void mergedQualifier(@TestQualifier OffsetScrollPosition offset);
	}
}
