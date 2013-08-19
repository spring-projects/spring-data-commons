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

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link PageableHandlerMethodArgumentResolver}. Pulls in defaulting tests from
 * {@link PageableDefaultUnitTests}.
 * 
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PageableHandlerMethodArgumentResolverUnitTests extends PageableDefaultUnitTests {

	@Test
	public void buildsUpRequestParameters() {

		String basicString = String.format("page=%d&size=%d", PAGE_NUMBER, PAGE_SIZE);

		assertUriStringFor(REFERENCE_WITHOUT_SORT, basicString);
		assertUriStringFor(REFERENCE_WITH_SORT, basicString + "&sort=firstname,lastname,desc");
		assertUriStringFor(REFERENCE_WITH_SORT_FIELDS, basicString + "&sort=firstname,lastname,asc");
	}

	/**
	 * @see DATACMNS-335
	 */
	@Test
	public void preventsPageSizeFromExceedingMayValueIfConfigured() throws Exception {

		// Write side
		assertUriStringFor(new PageRequest(0, 200), "page=0&size=100");

		// Read side
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "200");

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);

		assertSupportedAndResult(parameter, new PageRequest(0, 100), request);
	}

	/**
	 * @see DATACMNS-343
	 */
	@Test
	public void replacesExistingPaginationInformation() throws Exception {

		MethodParameter parameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
		UriComponentsContributor resolver = new HateoasPageableHandlerMethodArgumentResolver();
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080?page=0&size=10");
		resolver.enhance(builder, parameter, new PageRequest(1, 20));

		MultiValueMap<String, String> params = builder.build().getQueryParams();

		List<String> page = params.get("page");
		assertThat(page.size(), is(1));
		assertThat(page.get(0), is("1"));

		List<String> size = params.get("size");
		assertThat(size.size(), is(1));
		assertThat(size.get(0), is("20"));
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

	@Test(expected = IllegalArgumentException.class)
	public void pageResolverMustBeHateoasResolver() {
		new HateoasPageableHandlerMethodArgumentResolver().setSortResolver(new SortHandlerMethodArgumentResolver());
	}

	@Test
	public void pageResolverMayBeHateoasResolver() {
		new HateoasPageableHandlerMethodArgumentResolver()
				.setSortResolver(new HateoasSortHandlerMethodArgumentResolver());
	}

	@Test
	public void pageResolverDefaultsToHateoasResolver() {
		PageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
		assertSame(HateoasSortHandlerMethodArgumentResolver.class, resolver.getSortResolver().getClass());
	}

	@Test
	public void pageResolverMayBeNonHateoasResolver() {
		new PageableHandlerMethodArgumentResolver().setSortResolver(new SortHandlerMethodArgumentResolver());
	}

	@Test
	public void pageResolverDefaultsToNonHateoasResolver() {
		PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
		assertSame(SortHandlerMethodArgumentResolver.class, resolver.getSortResolver().getClass());
	}

	@Override
	protected HateoasPageableHandlerMethodArgumentResolver getResolver() {
		HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
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
