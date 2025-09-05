/*
 * Copyright 2017-2025 the original author or authors.
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
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.web.SortDefaultUnitTests.*;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link ReactiveSortHandlerMethodArgumentResolver}.
 *
 * @author Mark Paluch
 */
class ReactiveSortHandlerMethodArgumentResolverUnitTests {

	static MethodParameter PARAMETER;

	@BeforeAll
	static void setUp() throws Exception {
		PARAMETER = new MethodParameter(Controller.class.getMethod("supportedMethod", Sort.class), 0);
	}

	@Test // DATACMNS-1211
	void supportsSortParameter() {

		var resolver = new ReactiveSortHandlerMethodArgumentResolver();

		assertThat(resolver.supportsParameter(getParameterOfMethod("supportedMethod"))).isTrue();
	}

	@Test // DATACMNS-1211
	void returnsUnsortedForNoDefault() {
		assertSupportedAndResolvedTo(getParameterOfMethod("supportedMethod"), Sort.unsorted());
	}

	@Test // DATACMNS-1211
	void discoversSimpleDefault() {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefault"), Sort.by(SORT_FIELDS).ascending());
	}

	@Test // DATACMNS-1211
	void discoversSimpleDefaultWithDirection() {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefaultWithDirection"), SORT);
	}

	@Test // DATACMNS-1211
	void fallbackToGivenDefaultSort() {

		var parameter = TestUtils.getParameterOfMethod(Controller.class, "unsupportedMethod", String.class);
		var resolver = new ReactiveSortHandlerMethodArgumentResolver();
		var fallbackSort = Sort.by(Direction.ASC, "ID");
		resolver.setFallbackSort(fallbackSort);

		assertThat(resolve(resolver, TestUtils.getWebfluxRequest(), parameter)).isEqualTo(fallbackSort);
	}

	@Test // DATACMNS-1211
	void fallbackToDefaultDefaultSort() {

		var parameter = TestUtils.getParameterOfMethod(Controller.class, "unsupportedMethod", String.class);
		var resolver = new ReactiveSortHandlerMethodArgumentResolver();

		assertThat(resolve(resolver, TestUtils.getWebfluxRequest(), parameter).isSorted()).isFalse();
	}

	@Test // DATACMNS-1211
	void discoversSimpleSortFromRequest() {

		var parameter = getParameterOfMethod("simpleDefault");
		var reference = Sort.by("bar", "foo");
		var request = getRequestWithSort(reference);

		assertSupportedAndResolvedTo(request, parameter, reference);
	}

	@Test // DATACMNS-1211
	void discoversComplexSortFromRequest() {

		var parameter = getParameterOfMethod("simpleDefault");
		var reference = Sort.by("bar", "foo").and(Sort.by("fizz", "buzz"));

		assertSupportedAndResolvedTo(getRequestWithSort(reference), parameter, reference);
	}

	@Test // DATACMNS-1211
	void discoversQualifiedSortFromRequest() {

		var parameter = getParameterOfMethod("qualifiedSort");
		var reference = Sort.by("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, "qual"), parameter, reference);
	}

	@Test // DATACMNS-1211
	void requestForMultipleSortPropertiesIsUnmarshalledCorrectly() {

		var request = MockServerHttpRequest.get(String.format("foo?sort=%s", SortDefaultUnitTests.SORT_3))
				.build();

		var resolver = new ReactiveSortHandlerMethodArgumentResolver();
		var result = resolve(resolver, request, PARAMETER);

		assertThat(result).isEqualTo(Sort.by(Direction.ASC, "firstname", "lastname"));
	}

	@Test // DATACMNS-1211
	void parsesEmptySortToNull() {

		var request = MockServerHttpRequest.get("foo?sort=").build();

		assertThat(resolve(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-1211
	void sortParamIsInvalidProperty() {

		var request = MockServerHttpRequest.get("foo?sort=,DESC").build();

		assertThat(resolve(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-1211
	void sortParamIsInvalidPropertyWhenMultiProperty() {

		var request = MockServerHttpRequest.get("foo?sort=property1,,DESC").build();

		assertThat(resolve(request, PARAMETER)).isEqualTo(Sort.by(DESC, "property1"));
	}

	@Test // GH-2657
	void considersRepeatableAnnotation() {
		assertSupportedAndResolvedTo(getParameterOfMethod("repeatable"), Sort.by("one", "two", "three").ascending());
	}

	@Test // DATACMNS-1211
	void sortParamIsEmptyWhenMultiParams() {

		var request = MockServerHttpRequest.get("foo?sort=property,DESC&sort=").build();

		assertThat(resolve(request, PARAMETER)).isEqualTo(Sort.by(DESC, "property"));
	}

	@Test // DATACMNS-1211
	void parsesCommaParameterForSort() {

		var request = MockServerHttpRequest.get("foo?sort=,").build();

		assertThat(resolve(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-1211
	void doesNotReturnNullWhenAnnotatedWithSortDefault() {

		var request = MockServerHttpRequest.get("foo?sort=").build();

		assertThat(resolve(request, getParameterOfMethod("simpleDefault"))).isEqualTo(Sort.by("firstname", "lastname"));
		assertThat(resolve(request, getParameterOfMethod("containeredDefault"))).isEqualTo(Sort.by("foo", "bar"));
	}

	private static Sort resolve(MockServerHttpRequest request, MethodParameter parameter) {
		return resolve(new ReactiveSortHandlerMethodArgumentResolver(), request, parameter);
	}

	private static Sort resolve(ReactiveSortHandlerMethodArgumentResolver resolver, MockServerHttpRequest request,
			MethodParameter parameter) {
		return resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request));
	}

	private static void assertSupportedAndResolvedTo(MethodParameter parameter, Sort sort) {
		assertSupportedAndResolvedTo(TestUtils.getWebfluxRequest(), parameter, sort);
	}

	private static void assertSupportedAndResolvedTo(MockServerHttpRequest request, MethodParameter parameter,
			Sort sort) {

		var resolver = new ReactiveSortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		var resolved = resolve(resolver, request, parameter);

		assertThat(resolved).isEqualTo(sort);
	}

	private static MockServerHttpRequest getRequestWithSort(Sort sort) {
		return getRequestWithSort(sort, null);
	}

	private static MockServerHttpRequest getRequestWithSort(@Nullable Sort sort, @Nullable String qualifier) {

		if (sort == null) {
			return TestUtils.getWebfluxRequest();
		}

		var queryString = new StringBuilder();
		for (var order : sort) {

			var prefix = StringUtils.hasText(qualifier) ? qualifier + "_" : "";

			if (queryString.length() != 0) {
				queryString.append('&');
			}
			queryString.append(String.format("%ssort=%s,%s", prefix, order.getProperty(), order.getDirection().name()));
		}

		return MockServerHttpRequest.get(String.format("foo?%s", queryString.toString())).build();
	}

	private static MethodParameter getParameterOfMethod(String name) {
		return TestUtils.getParameterOfMethod(Controller.class, name, Sort.class);
	}

	interface Controller {

		void supportedMethod(Sort sort);

		void unsupportedMethod(String string);

		void qualifiedSort(@Qualifier("qual") Sort sort);

		void simpleDefault(@SortDefault({ "firstname", "lastname" }) Sort sort);

		void simpleDefaultWithDirection(
				@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Sort sort);

		void containeredDefault(@SortDefaults(@SortDefault({ "foo", "bar" })) Sort sort);

		void repeatable(@SortDefault({ "one", "two" }) @SortDefault({ "three" }) Sort sort);
	}
}
