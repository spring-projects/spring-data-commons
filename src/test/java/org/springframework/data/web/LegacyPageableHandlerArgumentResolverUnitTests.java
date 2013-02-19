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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.web.PageableHandlerMethodArgumentResolver.*;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link PageableHandlerMethodArgumentResolver} in it's legacy mode. Essentially a copy of
 * {@link PageableArgumentResolverUnitTests} but but executed against {@link PageableHandlerMethodArgumentResolver}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
@SuppressWarnings("deprecation")
public class LegacyPageableHandlerArgumentResolverUnitTests extends PageableDefaultUnitTest {

	Method correctMethod, noQualifiers, invalidQualifiers, defaultsMethod, defaultsMethodWithSort,
			defaultsMethodWithSortAndDirection, otherMethod;

	MockHttpServletRequest request;

	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		correctMethod = SampleController.class.getMethod("correctMethod", Pageable.class, Pageable.class);
		noQualifiers = SampleController.class.getMethod("noQualifiers", Pageable.class, Pageable.class);
		invalidQualifiers = SampleController.class.getMethod("invalidQualifiers", Pageable.class, Pageable.class);
		otherMethod = SampleController.class.getMethod("unsupportedMethod", String.class);

		defaultsMethod = SampleController.class.getMethod("simpleDefault", Pageable.class);
		defaultsMethodWithSort = SampleController.class.getMethod("simpleDefaultWithSort", Pageable.class);
		defaultsMethodWithSortAndDirection = SampleController.class.getMethod("simpleDefaultWithSortAndDirection",
				Pageable.class);

		request = new MockHttpServletRequest();

		// Add pagination info for foo table
		request.addParameter("foo_page.size", "50");
		request.addParameter("foo_page.sort", "foo");
		request.addParameter("foo_page.sort.dir", "asc");

		// Add pagination info for bar table
		request.addParameter("bar_page.size", "60");
	}

	@Test
	public void supportsPageableParameter() {

		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.supportsParameter(new MethodParameter(correctMethod, 0));
	}

	@Test
	public void doesNotSupportNonPageableParameter() {

		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		resolver.supportsParameter(new MethodParameter(otherMethod, 0));
	}

	@Test
	public void testname() throws Exception {

		assertSizeForPrefix(50, new Sort(Direction.ASC, "foo"), 0);
		assertSizeForPrefix(60, null, 1);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidlyMappedPageables() throws Exception {

		MethodParameter parameter = new MethodParameter(noQualifiers, 0);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		new PageableHandlerMethodArgumentResolver().resolveArgument(parameter, null, webRequest, null);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidQualifiers() throws Exception {

		MethodParameter parameter = new MethodParameter(invalidQualifiers, 0);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		new PageableHandlerMethodArgumentResolver().resolveArgument(parameter, null, webRequest, null);
	}

	@Test
	public void assertDefaults() throws Exception {

		Object argument = setupAndResolve(defaultsMethod);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(PAGE_SIZE));
		assertThat(pageable.getPageNumber(), is(PAGE_NUMBER));
		assertThat(pageable.getSort(), is(nullValue()));
	}

	@Test
	public void assertOverridesDefaults() throws Exception {

		Integer sizeParam = 5;

		MethodParameter parameter = new MethodParameter(defaultsMethod, 0);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();

		mockRequest.addParameter("page.page", sizeParam.toString());
		NativeWebRequest webRequest = new ServletWebRequest(mockRequest);
		Object argument = LEGACY.resolveArgument(parameter, null, webRequest, null);

		assertTrue(argument instanceof Pageable);

		Pageable pageable = (Pageable) argument;
		assertEquals(PAGE_SIZE, pageable.getPageSize());
		assertEquals(sizeParam - 1, pageable.getPageNumber());
	}

	@Test
	public void appliesDefaultSort() throws Exception {

		Object argument = setupAndResolve(defaultsMethodWithSort);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(PAGE_SIZE));
		assertThat(pageable.getPageNumber(), is(PAGE_NUMBER));
		assertThat(pageable.getSort(), is(new Sort("firstname", "lastname")));
	}

	@Test
	public void appliesDefaultSortAndDirection() throws Exception {

		Object argument = setupAndResolve(defaultsMethodWithSortAndDirection);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(PAGE_SIZE));
		assertThat(pageable.getPageNumber(), is(PAGE_NUMBER));
		assertThat(pageable.getSort(), is(new Sort(Direction.DESC, "firstname", "lastname")));
	}

	@Test
	public void buildsUpRequestParameters() {

		// Set up basic page representation based on 1-indexed page numbers
		String basicString = String.format("page.page=%d&page.size=%d", PAGE_NUMBER + 1, PAGE_SIZE);

		assertUriStringFor(REFERENCE_WITHOUT_SORT, basicString);
		assertUriStringFor(REFERENCE_WITH_SORT, basicString + "&page.sort=firstname,lastname&page.sort.dir=desc");
		assertUriStringFor(REFERENCE_WITH_SORT_FIELDS, basicString + "&page.sort=firstname,lastname&page.sort.dir=asc");
	}

	private void assertSizeForPrefix(int size, Sort sort, int index) throws Exception {

		MethodParameter parameter = new MethodParameter(correctMethod, index);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		Object argument = LEGACY.resolveArgument(parameter, null, webRequest, null);
		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(size));

		if (null != sort) {
			assertThat(pageable.getSort(), is(sort));
		}
	}

	private Object setupAndResolve(Method method) throws Exception {

		MethodParameter parameter = new MethodParameter(method, 0);
		NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

		return LEGACY.resolveArgument(parameter, null, webRequest, null);
	}

	@Override
	protected Class<?> getControllerClass() {
		return SampleController.class;
	}

	@Override
	protected PageableHandlerMethodArgumentResolver getResolver() {
		return PageableHandlerMethodArgumentResolver.LEGACY;
	}

	static interface SampleController {

		void simpleDefault(@PageableDefaults(value = PAGE_SIZE, pageNumber = PAGE_NUMBER) Pageable pageable);

		void simpleDefaultWithSort(@PageableDefaults(value = PAGE_SIZE, pageNumber = PAGE_NUMBER, sort = { "firstname",
				"lastname" }) Pageable pageable);

		void simpleDefaultWithSortAndDirection(@PageableDefaults(value = PAGE_SIZE, pageNumber = PAGE_NUMBER, sort = {
				"firstname", "lastname" }, sortDir = Direction.DESC) Pageable pageable);

		void simpleDefaultWithExternalSort(
				@PageableDefaults(value = PAGE_SIZE, pageNumber = PAGE_NUMBER) @SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Pageable pageable);

		void simpleDefaultWithContaineredExternalSort(
				@PageableDefaults(value = PAGE_SIZE, pageNumber = PAGE_NUMBER) @SortDefaults(@SortDefault(sort = { "firstname",
						"lastname" }, direction = Direction.DESC)) Pageable pageable);

		void correctMethod(@Qualifier("foo") Pageable first, @Qualifier("bar") Pageable second);

		void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second);

		void noQualifiers(Pageable first, Pageable second);

		void supportedMethod(Pageable pageable);

		void unsupportedMethod(String foo);
	}
}
