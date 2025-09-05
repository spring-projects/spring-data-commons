/*
 * Copyright 2023-2025 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link OffsetScrollPositionHandlerMethodArgumentResolver}.
 *
 * @author Yanming Zhou
 * @author Mark Paluch
 */
class OffsetScrollPositionHandlerMethodArgumentResolverUnitTests {

	static final MethodParameter PARAMETER = getParameterOfMethod("supportedMethod");
	static final MethodParameter OPTIONAL_PARAMETER = getOptionalParameterOfMethod("supportedMethodWithOptional");

	final OffsetScrollPositionHandlerMethodArgumentResolver sut = new OffsetScrollPositionHandlerMethodArgumentResolver();

	@Test // GH-2856
	void supportsSortParameter() {
		assertThat(sut.supportsParameter(PARAMETER)).isTrue();
	}

	@Test // GH-2856
	void supportsOptionalOffsetScrollPositionParameter() {
		assertThat(sut.supportsParameter(OPTIONAL_PARAMETER)).isTrue();
	}

	@Test // GH-2856
	void discoversOffsetFromRequest() {

		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, null), PARAMETER, reference);
	}

	@Test // GH-2856
	void returnsNullIfNotSpecified() {

		var request = new MockHttpServletRequest();
		var position = resolveOffset(request, PARAMETER);

		assertThat(position).isNull();
	}

	@Test // GH-2856
	void returnsOptionalParameterFromRequest() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "5");
		var position = resolveOffset(request, OPTIONAL_PARAMETER);

		assertThat(position).isEqualTo(Optional.of(ScrollPosition.offset(5)));
	}

	@Test // GH-2856
	void returnsEmptyOptionalIfNotSpecified() {

		var request = new MockHttpServletRequest();
		var position = resolveOffset(request, OPTIONAL_PARAMETER);

		assertThat(position).isEqualTo(Optional.empty());
	}

	@Test // GH-2856
	void discoversOffsetFromRequestWithMultipleParams() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "5");
		request.addParameter("offset", "6");

		assertThat(resolveOffset(request, PARAMETER)).isEqualTo(ScrollPosition.offset(5));
	}

	@Test // GH-2856
	void discoversQualifiedOffsetFromRequest() {

		var parameter = getParameterOfMethod("qualifiedOffset");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, "hello"), parameter, reference);
	}

	@Test // GH-2856
	void returnsNullForOffsetParamSetToNothing() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", (String) null);

		assertThat(resolveOffset(request, PARAMETER)).isNull();
	}

	@Test // GH-2856
	void returnsNullForEmptyOffsetParam() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "");

		assertThat(resolveOffset(request, PARAMETER)).isNull();
	}

	@Test // GH-2856
	void returnsNullForOffsetParamIsInvalidProperty() {

		var request = new MockHttpServletRequest();
		request.addParameter("offset", "invalid_number");

		assertThat(resolveOffset(request, PARAMETER)).isNull();
	}

	@Test // GH-2856
	void emptyQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("emptyQualifier");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, ""), parameter, reference);
	}

	@Test // GH-2856
	void mergedQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("mergedQualifier");
		var reference = ScrollPosition.offset(5);

		assertSupportedAndResolvedTo(getRequestWithOffset(reference, "merged"), parameter, reference);
	}

	@Nullable
	private Object resolveOffset(HttpServletRequest request, MethodParameter parameter) {
		return sut.resolveArgument(parameter, null, new ServletWebRequest(request), null);
	}

	private void assertSupportedAndResolvedTo(NativeWebRequest request, MethodParameter parameter,
			OffsetScrollPosition position) {

		assertThat(sut.supportsParameter(parameter)).isTrue();
		assertThat(sut.resolveArgument(parameter, null, request, null)).isEqualTo(position);
	}

	private static MethodParameter getParameterOfMethod(String name) {
		return TestUtils.getParameterOfMethod(Controller.class, name, OffsetScrollPosition.class);
	}

	private static MethodParameter getOptionalParameterOfMethod(String name) {
		return TestUtils.getParameterOfMethod(Controller.class, name, Optional.class);
	}

	private static NativeWebRequest getRequestWithOffset(@Nullable OffsetScrollPosition position,
			@Nullable String qualifier) {

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

		void supportedMethodWithOptional(Optional<OffsetScrollPosition> offset);

		void unsupportedMethod(String string);

		void qualifiedOffset(@Qualifier("hello") OffsetScrollPosition offset);

		void emptyQualifier(@Qualifier OffsetScrollPosition offset);

		void mergedQualifier(@TestQualifier OffsetScrollPosition offset);
	}
}
