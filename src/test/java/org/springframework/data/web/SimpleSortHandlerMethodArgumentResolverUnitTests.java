/*
 * Copyright 2013-2015 the original author or authors.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.data.domain.Sort.Direction.*;

/**
 * Unit tests for {@link SimpleSortHandlerMethodArgumentResolver}.
 *
 * @since 1.6
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 * @author Muhammad Ichsan
 */
public class SimpleSortHandlerMethodArgumentResolverUnitTests extends SortDefaultUnitTests {

	static MethodParameter PARAMETER;

	static final String SORT_0 = "username";
	static final String SORT_1 = "firstname,lastname";
	static final String SORT_2 = "-created,title";
	static final String SORT_3 = "username,asc";

	@BeforeClass
	public static void setUp() throws Exception {
		PARAMETER = new MethodParameter(Controller.class.getMethod("supportedMethod", Sort.class), 0);
	}

	@Override
	protected HandlerMethodArgumentResolver getResolver() {
		return new SimpleSortHandlerMethodArgumentResolver();
	}

	/**
	 * @see DATACMNS-531
	 */
	@Test
	public void parsesSimpleSortStringCorrectly() {
		assertSortStringParsedInto(new Sort(new Order("username")), SORT_0);
		assertSortStringParsedInto(new Sort("firstname", "lastname"), SORT_1);
		assertSortStringParsedInto(new Sort(new Order(DESC, "created"), new Order(ASC, "title")), SORT_2);
		assertSortStringParsedInto(new Sort("username", "asc"), SORT_3);
	}

	private static void assertSortStringParsedInto(Sort expected, String source) {
		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		Sort sort = resolver.parseParameterIntoSort(source, ",");

		assertThat(sort, is(expected));
	}

	/**
	 * @see DATACMNS-531
	 */
	@Test
	public void supportsCustomAscendingSign() {
		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		resolver.setAscendingSign("+");
		Sort sort = resolver.parseParameterIntoSort("+name,-created,-modified,+subject", ",");

		assertThat(sort, is(new Sort(new Order("name"), new Order(DESC, "created"),
				new Order(DESC, "modified"), new Order("subject"))));
	}

	/**
	 * @see DATACMNS-351
	 */
	@Test
	public void fallbackToGivenDefaultSort() throws Exception {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		Sort fallbackSort = new Sort(Direction.ASC, "ID");
		resolver.setFallbackSort(fallbackSort);

		Sort sort = resolver.resolveArgument(parameter, null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertThat(sort, is(fallbackSort));
	}

	/**
	 * @see DATACMNS-351
	 */
	@Test
	public void fallbackToDefaultDefaultSort() throws Exception {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();

		Sort sort = resolver.resolveArgument(parameter, null, new ServletWebRequest(new MockHttpServletRequest()), null);
		assertThat(sort, is(nullValue()));
	}

	@Test
	public void discoversSimpleSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("simpleDefault");
		Sort reference = new Sort("bar", "foo");
		NativeWebRequest request = getRequestWithSort(reference);

		assertSupportedAndResolvedTo(request, parameter, reference);
	}

	@Test
	public void discoversComplexSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("simpleDefault");
		Sort reference = new Sort("bar", "foo").and(new Sort("fizz", "buzz"));

		assertSupportedAndResolvedTo(getRequestWithSort(reference), parameter, reference);
	}

	@Test
	public void discoversQualifiedSortFromRequest() {

		MethodParameter parameter = getParameterOfMethod("qualifiedSort");
		Sort reference = new Sort("bar", "foo");

		assertSupportedAndResolvedTo(getRequestWithSort(reference, "qual"), parameter, reference);
	}

	@Test
	public void returnsNullForSortParameterSetToNothing() throws Exception {

		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", (String) null);

		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		Sort result = resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
		assertThat(result, is(nullValue()));
	}

	/**
	 * @see DATACMNS-366
	 * @throws Exception
	 */
	@Test
	public void requestForMultipleSortPropertiesIsUnmarshalledCorrectly() throws Exception {

		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", SORT_1);

		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		Sort result = resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
		assertThat(result, is(new Sort(Direction.ASC, "firstname", "lastname")));
	}

	/**
	 * @see DATACMNS-408
	 */
	@Test
	public void parsesEmptySortToNull() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER), is(nullValue()));
	}

	/**
	 * @see DATACMNS-408
	 */
	@Test
	public void sortParamIsInvalidProperty() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", "-");

		assertThat(resolveSort(request, PARAMETER), is(nullValue()));
	}

	/**
	 * @see DATACMNS-408
	 */
	@Test
	public void sortParamIsInvalidPropertyWhenMultiProperty() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", "-property1,,");

		assertThat(resolveSort(request, PARAMETER), is(new Sort(DESC, "property1")));
	}

	@Test
	public void sortParamOnlyTakeTheFirstSort() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", "-property");
		request.addParameter("sort", "");

		assertThat(resolveSort(request, PARAMETER), is(new Sort(DESC, "property")));
	}

	/**
	 * @see DATACMNS-379
	 */
	@Test
	public void parsesCommaParameterForSort() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", ",");

		assertThat(resolveSort(request, PARAMETER), is(nullValue()));
	}

	/**
	 * @see DATACMNS-753, DATACMNS-408
	 */
	@Test
	public void doesNotReturnNullWhenAnnotatedWithSortDefault() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("sort", "");

		assertThat(resolveSort(request, getParameterOfMethod("simpleDefault")), is(new Sort("firstname", "lastname")));
		assertThat(resolveSort(request, getParameterOfMethod("containeredDefault")), is(new Sort("foo", "bar")));
	}

	private static Sort resolveSort(HttpServletRequest request, MethodParameter parameter) throws Exception {

		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		return resolver.resolveArgument(parameter, null, new ServletWebRequest(request), null);
	}

	private static void assertSupportedAndResolvedTo(NativeWebRequest request, MethodParameter parameter, Sort sort) {

		SimpleSortHandlerMethodArgumentResolver resolver = new SimpleSortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter), is(true));

		try {
			assertThat(resolver.resolveArgument(parameter, null, request, null), is(sort));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static NativeWebRequest getRequestWithSort(Sort sort) {
		return getRequestWithSort(sort, null);
	}

	// Specialized for DATACMNS-531
	private static NativeWebRequest getRequestWithSort(Sort sort, String qualifier) {

		MockHttpServletRequest request = new MockHttpServletRequest();

		if (sort == null) {
			return new ServletWebRequest(request);
		}

		String prefix = StringUtils.hasText(qualifier) ? qualifier + "_" : "";
		List<String> construction = new ArrayList<String>();
		for (Order order : sort) {
			String sign = "";
			if (order.getDirection() == DESC) {
				sign = "-";
			}
			construction.add(String.format("%s%s", sign, order.getProperty()));
		}

		request.addParameter(prefix + "sort", StringUtils.arrayToDelimitedString(construction.toArray(), ","));

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

		void simpleDefault(@SortDefault({"firstname", "lastname"}) Sort sort);

		void simpleDefaultWithDirection(
				@SortDefault(sort = {"firstname", "lastname"}, direction = Direction.DESC) Sort sort);

		void containeredDefault(@SortDefaults(@SortDefault({"foo", "bar"})) Sort sort);

		void invalid(@SortDefaults(@SortDefault({"foo", "bar"})) @SortDefault({"bar", "foo"}) Sort sort);
	}
}
