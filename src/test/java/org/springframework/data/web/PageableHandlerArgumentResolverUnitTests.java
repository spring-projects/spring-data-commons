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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link PageableHandlerArgumentResolver}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public class PageableHandlerArgumentResolverUnitTests {

	Method correctMethod, failedMethod, invalidQualifiers, defaultsMethod, defaultsMethodWithSort,
			defaultsMethodWithSortAndDirection, otherMethod;

	MockHttpServletRequest request;

	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		correctMethod = SampleController.class.getMethod("correctMethod", Pageable.class, Pageable.class);
		failedMethod = SampleController.class.getMethod("failedMethod", Pageable.class, Pageable.class);
		invalidQualifiers = SampleController.class.getMethod("invalidQualifiers", Pageable.class, Pageable.class);
		otherMethod = SampleController.class.getMethod("otherMethod", String.class);

		defaultsMethod = SampleController.class.getMethod("defaultsMethod", Pageable.class);
		defaultsMethodWithSort = SampleController.class.getMethod("defaultsMethodWithSort", Pageable.class);
		defaultsMethodWithSortAndDirection = SampleController.class.getMethod("defaultsMethodWithSortAndDirection",
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

		PageableHandlerArgumentResolver resolver = new PageableHandlerArgumentResolver();
		resolver.supportsParameter(new MethodParameter(correctMethod, 0));
	}

	@Test
	public void doesNotSupportNonPageableParameter() {

		PageableHandlerArgumentResolver resolver = new PageableHandlerArgumentResolver();
		resolver.supportsParameter(new MethodParameter(otherMethod, 0));
	}

	@Test
	public void testname() throws Exception {

		assertSizeForPrefix(50, new Sort(Direction.ASC, "foo"), 0);
		assertSizeForPrefix(60, null, 1);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidlyMappedPageables() throws Exception {

		MethodParameter parameter = new MethodParameter(failedMethod, 0);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		new PageableHandlerArgumentResolver().resolveArgument(parameter, null, webRequest, null);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidQualifiers() throws Exception {

		MethodParameter parameter = new MethodParameter(invalidQualifiers, 0);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		new PageableHandlerArgumentResolver().resolveArgument(parameter, null, webRequest, null);
	}

	@Test
	public void assertDefaults() throws Exception {

		Object argument = setupAndResolve(defaultsMethod);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(SampleController.DEFAULT_PAGESIZE));
		assertThat(pageable.getPageNumber(), is(SampleController.DEFAULT_PAGENUMBER));
		assertThat(pageable.getSort(), is(nullValue()));
	}

	@Test
	public void assertOverridesDefaults() throws Exception {

		Integer sizeParam = 5;

		MethodParameter parameter = new MethodParameter(defaultsMethod, 0);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();

		mockRequest.addParameter("page.page", sizeParam.toString());
		NativeWebRequest webRequest = new ServletWebRequest(mockRequest);
		Object argument = new PageableHandlerArgumentResolver().resolveArgument(parameter, null, webRequest, null);

		assertTrue(argument instanceof Pageable);

		Pageable pageable = (Pageable) argument;
		assertEquals(SampleController.DEFAULT_PAGESIZE, pageable.getPageSize());
		assertEquals(sizeParam - 1, pageable.getPageNumber());
	}

	@Test
	public void appliesDefaultSort() throws Exception {

		Object argument = setupAndResolve(defaultsMethodWithSort);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(SampleController.DEFAULT_PAGESIZE));
		assertThat(pageable.getPageNumber(), is(SampleController.DEFAULT_PAGENUMBER));
		assertThat(pageable.getSort(), is(new Sort("foo")));
	}

	@Test
	public void appliesDefaultSortAndDirection() throws Exception {

		Object argument = setupAndResolve(defaultsMethodWithSortAndDirection);

		assertThat(argument, is(instanceOf(Pageable.class)));

		Pageable pageable = (Pageable) argument;
		assertThat(pageable.getPageSize(), is(SampleController.DEFAULT_PAGESIZE));
		assertThat(pageable.getPageNumber(), is(SampleController.DEFAULT_PAGENUMBER));
		assertThat(pageable.getSort(), is(new Sort(Direction.DESC, "foo")));
	}

	private void assertSizeForPrefix(int size, Sort sort, int index) throws Exception {

		MethodParameter parameter = new MethodParameter(correctMethod, index);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		Object argument = new PageableHandlerArgumentResolver().resolveArgument(parameter, null, webRequest, null);
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
		return new PageableHandlerArgumentResolver().resolveArgument(parameter, null, webRequest, null);
	}

	static class SampleController {

		static final int DEFAULT_PAGESIZE = 198;
		static final int DEFAULT_PAGENUMBER = 42;

		public void defaultsMethod(
				@PageableDefaults(value = DEFAULT_PAGESIZE, pageNumber = DEFAULT_PAGENUMBER) Pageable pageable) {

		}

		public void defaultsMethodWithSort(
				@PageableDefaults(value = DEFAULT_PAGESIZE, pageNumber = DEFAULT_PAGENUMBER, sort = "foo") Pageable pageable) {

		}

		public void defaultsMethodWithSortAndDirection(
				@PageableDefaults(value = DEFAULT_PAGESIZE, pageNumber = DEFAULT_PAGENUMBER, sort = "foo", sortDir = Direction.DESC) Pageable pageable) {

		}

		public void correctMethod(@Qualifier("foo") Pageable first, @Qualifier("bar") Pageable second) {

		}

		public void failedMethod(Pageable first, Pageable second) {

		}

		public void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second) {

		}

		public void otherMethod(String foo) {

		}
	}
}
