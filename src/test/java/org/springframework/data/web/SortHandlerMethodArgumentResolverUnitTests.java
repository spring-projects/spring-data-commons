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
import static org.springframework.data.domain.Sort.Direction.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link SortHandlerMethodArgumentResolver}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 * @author Mark Paluch
 * @author Vedran Pavic
 */
class SortHandlerMethodArgumentResolverUnitTests extends SortDefaultUnitTests {

	static MethodParameter PARAMETER;

	@BeforeAll
	static void setUp() throws Exception {
		PARAMETER = new MethodParameter(Controller.class.getMethod("supportedMethod", Sort.class), 0);
	}

	@Test // DATACMNS-351
	void fallbackToGivenDefaultSort() {

		var parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		var resolver = new SortHandlerMethodArgumentResolver();
		var fallbackSort = Sort.by(Direction.ASC, "ID");
		resolver.setFallbackSort(fallbackSort);

		var sort = resolver.resolveArgument(parameter, null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertThat(sort).isEqualTo(fallbackSort);
	}

	@Test // DATACMNS-351
	void fallbackToDefaultDefaultSort() {

		var parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		var resolver = new SortHandlerMethodArgumentResolver();

		var sort = resolver.resolveArgument(parameter, null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertThat(sort.isSorted()).isFalse();
	}

	@Test
	void discoversSimpleSortFromRequest() {

		var parameter = getParameterOfMethod("simpleDefault");
		var reference = Sort.by("bar", "foo");
		var request = getRequestWithSort(reference);

		assertSupportedAndResolvedTo(request, parameter, reference);
	}

	@Test
	void discoversComplexSortFromRequest() {

		var parameter = getParameterOfMethod("simpleDefault");
		var reference = Sort.by("bar", "foo").and(Sort.by("fizz", "buzz"));

		assertSupportedAndResolvedTo(getRequestWithSort(reference), parameter, reference);
	}

	@Test
	void discoversQualifiedSortFromRequest() {

		var parameter = getParameterOfMethod("qualifiedSort");
		var reference = Sort.by("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, "qual"), parameter, reference);
	}

	@Test
	void returnsNullForSortParameterSetToNothing() {

		var parameter = getParameterOfMethod("supportedMethod");

		var request = new MockHttpServletRequest();
		request.addParameter("sort", (String) null);

		var resolver = new SortHandlerMethodArgumentResolver();
		var result = resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
		assertThat(result.isSorted()).isFalse();
	}

	@Test // DATACMNS-366
	void requestForMultipleSortPropertiesIsUnmarshalledCorrectly() {

		var parameter = getParameterOfMethod("supportedMethod");

		var request = new MockHttpServletRequest();
		request.addParameter("sort", SORT_3);

		var resolver = new SortHandlerMethodArgumentResolver();
		var result = resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
		assertThat(result).isEqualTo(Sort.by(Direction.ASC, "firstname", "lastname"));
	}

	@Test // DATACMNS-408
	void parsesEmptySortToNull() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-408
	void sortParamIsInvalidProperty() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", ",DESC");

		assertThat(resolveSort(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-408
	void sortParamIsInvalidPropertyWhenMultiProperty() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "property1,,DESC");

		assertThat(resolveSort(request, PARAMETER)).isEqualTo(Sort.by(DESC, "property1"));
	}

	@Test // DATACMNS-408
	void sortParamIsEmptyWhenMultiParams() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "property,DESC");
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER)).isEqualTo(Sort.by(DESC, "property"));
	}

	@Test // DATACMNS-658
	void sortParamHandlesSortOrderAndIgnoreCase() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "property,DESC,IgnoreCase");
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER)).isEqualTo(Sort.by(new Order(DESC, "property").ignoreCase()));
	}

	@Test // DATACMNS-658
	void sortParamHandlesMultiplePropertiesWithSortOrderAndIgnoreCase() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "property1,property2,DESC,IgnoreCase");

		assertThat(resolveSort(request, PARAMETER))
				.isEqualTo(Sort.by(new Order(DESC, "property1").ignoreCase(), new Order(DESC, "property2").ignoreCase()));
	}

	@Test // DATACMNS-658
	void sortParamHandlesIgnoreCase() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "property,IgnoreCase");
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER)).isEqualTo(Sort.by(new Order(ASC, "property").ignoreCase()));
	}

	@Test // DATACMNS-658
	void returnsDefaultCaseInsensitive() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "");

		assertThat(resolveSort(request, getParameterOfMethod("simpleDefaultWithDirectionCaseInsensitive")))
				.isEqualTo(Sort.by(new Order(DESC, "firstname").ignoreCase(), new Order(DESC, "lastname").ignoreCase()));
	}

	@Test // DATACMNS-379
	void parsesCommaParameterForSort() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", ",");

		assertThat(resolveSort(request, PARAMETER).isSorted()).isFalse();
	}

	@Test // DATACMNS-753, DATACMNS-408
	void doesNotReturnNullWhenAnnotatedWithSortDefault() throws Exception {

		var request = new MockHttpServletRequest();
		request.addParameter("sort", "");

		assertThat(resolveSort(request, getParameterOfMethod("simpleDefault"))).isEqualTo(Sort.by("firstname", "lastname"));
		assertThat(resolveSort(request, getParameterOfMethod("containeredDefault"))).isEqualTo(Sort.by("foo", "bar"));
	}

	@Test // DATACMNS-1551
	void resolvesDotOnlyInputToDefault() {

		Stream.of(".", ".,ASC").forEach(it -> {

			var request = new MockHttpServletRequest();
			request.addParameter("sort", it);

			assertThatCode(() -> {
				assertThat(resolveSort(request, PARAMETER)).isEqualTo(Sort.unsorted());
			}).doesNotThrowAnyException();
		});
	}

	@Test // DATACMNS-1827
	void emptyQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("emptyQualifier");
		var reference = Sort.by("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, ""), parameter, reference);
	}

	@Test // DATACMNS-1827
	void mergedQualifierIsUsedInParameterLookup() {

		var parameter = getParameterOfMethod("mergedQualifier");
		var reference = Sort.by("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, "merged"), parameter, reference);
	}

	private static Sort resolveSort(HttpServletRequest request, MethodParameter parameter) throws Exception {

		var resolver = new SortHandlerMethodArgumentResolver();
		return resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
	}

	private static void assertSupportedAndResolvedTo(NativeWebRequest request, MethodParameter parameter, Sort sort) {

		var resolver = new SortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter)).isTrue();

		try {
			assertThat(resolver.resolveArgument(parameter, null, request, null)).isEqualTo(sort);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static NativeWebRequest getRequestWithSort(Sort sort) {
		return getRequestWithSort(sort, null);
	}

	private static NativeWebRequest getRequestWithSort(@Nullable Sort sort, @Nullable String qualifier) {

		var request = new MockHttpServletRequest();

		if (sort == null) {
			return new ServletWebRequest(request);
		}

		for (var order : sort) {

			var prefix = StringUtils.hasText(qualifier) ? qualifier + "_" : "";
			var suffix = order.isIgnoreCase() ? ",IgnoreCase" : "";
			request.addParameter(prefix + "sort",
					String.format("%s,%s%s", order.getProperty(), order.getDirection().name(), suffix));
		}

		return new ServletWebRequest(request);
	}

	@Override
	protected Class<?> getControllerClass() {
		return Controller.class;
	}

	interface Controller {

		void supportedMethod(Sort sort);

		void unsupportedMethod(String string);

		void qualifiedSort(@Qualifier("qual") Sort sort);

		void simpleDefault(@SortDefault({ "firstname", "lastname" }) Sort sort);

		void simpleDefaultWithDirection(
				@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Sort sort);

		void simpleDefaultWithDirectionCaseInsensitive(
				@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC, caseSensitive = false) Sort sort);

		void containeredDefault(@SortDefaults(@SortDefault({ "foo", "bar" })) Sort sort);

		void invalid(@SortDefaults(@SortDefault({ "foo", "bar" })) @SortDefault({ "bar", "foo" }) Sort sort);

		void emptyQualifier(@Qualifier Sort sort);

		void mergedQualifier(@TestQualifier Sort sort);
	}
}
