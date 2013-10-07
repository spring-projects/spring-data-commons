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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link PageableHandlerMethodArgumentResolver}. Pulls in defaulting tests from
 * {@link PageableDefaultUnitTests}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PageableHandlerMethodArgumentResolverUnitTests extends PageableDefaultUnitTests {

	MethodParameter supportedMethodParameter;

	@Before
	public void setUp() throws Exception {
		this.supportedMethodParameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
	}

	/**
	 * @see DATACMNS-335
	 */
	@Test
	public void preventsPageSizeFromExceedingMayValueIfConfigured() throws Exception {

		// Read side
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "200");

		assertSupportedAndResult(supportedMethodParameter, new PageRequest(0, 100), request);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyPageParameterName() {
		new PageableHandlerMethodArgumentResolver().setPageParameterName("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPageParameterName() {
		new PageableHandlerMethodArgumentResolver().setPageParameterName(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptySizeParameterName() {
		new PageableHandlerMethodArgumentResolver().setSizeParameterName("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullSizeParameterName() {
		new PageableHandlerMethodArgumentResolver().setSizeParameterName(null);
	}

	@Test
	public void qualifierIsUsedInParameterLookup() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("validQualifier", Pageable.class), 0);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo_page", "2");
		request.addParameter("foo_size", "10");

		assertSupportedAndResult(parameter, new PageRequest(2, 10), request);
	}

	/**
	 * @see DATACMNS-377
	 */
	@Test
	public void usesDefaultPageSizeIfRequestPageSizeIsLessThanOne() throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "0");

		assertSupportedAndResult(supportedMethodParameter, PageableHandlerMethodArgumentResolver.DEFAULT_PAGE_REQUEST,
				request);
	}

	/**
	 * @see DATACMNS-377
	 */
	@Test
	public void rejectsInvalidCustomDefaultForPageSize() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("invalidDefaultPageSize", Pageable.class), 0);

		exception.expect(IllegalStateException.class);
		exception.expectMessage("invalidDefaultPageSize");

		assertSupportedAndResult(parameter, PageableHandlerMethodArgumentResolver.DEFAULT_PAGE_REQUEST);
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

		void simpleDefaultWithSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER,
				sort = { "firstname", "lastname" }) Pageable pageable);

		void simpleDefaultWithSortAndDirection(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER, sort = { "firstname",
				"lastname" }, direction = Direction.DESC) Pageable pageable);

		void simpleDefaultWithExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER)//
				@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC) Pageable pageable);

		void simpleDefaultWithContaineredExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER)//
				@SortDefaults(@SortDefault(sort = { "firstname", "lastname" }, direction = Direction.DESC)) Pageable pageable);

		void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second);

		void validQualifier(@Qualifier("foo") Pageable pageable);

		void noQualifiers(Pageable first, Pageable second);
	}
}
