/*
 * Copyright 2008-2011 the original author or authors.
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
 * Unit test for {@link PageableArgumentResolver}.
 * 
 * @author Oliver Gierke - gierke@synyx.de
 */
public class PageableArgumentResolverUnitTests {

	Method correctMethod;
	Method failedMethod;
	Method invalidQualifiers;
	Method defaultsMethod;

	MockHttpServletRequest request;

	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		correctMethod = SampleController.class.getMethod("correctMethod", Pageable.class, Pageable.class);
		failedMethod = SampleController.class.getMethod("failedMethod", Pageable.class, Pageable.class);
		invalidQualifiers = SampleController.class.getMethod("invalidQualifiers", Pageable.class, Pageable.class);

		defaultsMethod = SampleController.class.getMethod("defaultsMethod", Pageable.class);

		request = new MockHttpServletRequest();

		// Add pagination info for foo table
		request.addParameter("foo_page.size", "50");
		request.addParameter("foo_page.sort", "foo");
		request.addParameter("foo_page.sort.dir", "asc");

		// Add pagination info for bar table
		request.addParameter("bar_page.size", "60");
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

		new PageableArgumentResolver().resolveArgument(parameter, webRequest);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidQualifiers() throws Exception {

		MethodParameter parameter = new MethodParameter(invalidQualifiers, 0);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		new PageableArgumentResolver().resolveArgument(parameter, webRequest);
	}

	@Test
	public void assertDefaults() throws Exception {

		MethodParameter parameter = new MethodParameter(defaultsMethod, 0);
		NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());
		PageableArgumentResolver resolver = new PageableArgumentResolver();
		Object argument = resolver.resolveArgument(parameter, webRequest);

		assertTrue(argument instanceof Pageable);

		Pageable pageable = (Pageable) argument;
		assertEquals(SampleController.DEFAULT_PAGESIZE, pageable.getPageSize());
		assertEquals(SampleController.DEFAULT_PAGENUMBER, pageable.getPageNumber());
	}

	@Test
	public void assertOverridesDefaults() throws Exception {

		Integer sizeParam = 5;

		MethodParameter parameter = new MethodParameter(defaultsMethod, 0);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();

		mockRequest.addParameter("page.page", sizeParam.toString());
		NativeWebRequest webRequest = new ServletWebRequest(mockRequest);
		PageableArgumentResolver resolver = new PageableArgumentResolver();
		Object argument = resolver.resolveArgument(parameter, webRequest);

		assertTrue(argument instanceof Pageable);

		Pageable pageable = (Pageable) argument;
		assertEquals(SampleController.DEFAULT_PAGESIZE, pageable.getPageSize());
		assertEquals(sizeParam - 1, pageable.getPageNumber());
	}

	private void assertSizeForPrefix(int size, Sort sort, int index) throws Exception {

		MethodParameter parameter = new MethodParameter(correctMethod, index);
		NativeWebRequest webRequest = new ServletWebRequest(request);

		PageableArgumentResolver resolver = new PageableArgumentResolver();

		Object argument = resolver.resolveArgument(parameter, webRequest);
		assertTrue(argument instanceof Pageable);

		Pageable pageable = (Pageable) argument;
		assertEquals(size, pageable.getPageSize());

		if (null != sort) {
			assertEquals(sort, pageable.getSort());
		}
	}

	@SuppressWarnings("unused")
	private class SampleController {

		static final int DEFAULT_PAGESIZE = 198;
		static final int DEFAULT_PAGENUMBER = 42;

		public void defaultsMethod(
				@PageableDefaults(value = DEFAULT_PAGESIZE, pageNumber = DEFAULT_PAGENUMBER) Pageable pageable) {

		}

		public void correctMethod(@Qualifier("foo") Pageable first, @Qualifier("bar") Pageable second) {

		}

		public void failedMethod(Pageable first, Pageable second) {

		}

		public void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second) {

		}
	}
}
