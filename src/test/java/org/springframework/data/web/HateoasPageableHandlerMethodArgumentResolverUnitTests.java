/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.mvc.UriComponentsContributor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HateoasPageableHandlerMethodArgumentResolver}.
 * 
 * @author Oliver Gierke
 */
public class HateoasPageableHandlerMethodArgumentResolverUnitTests
		extends PageableHandlerMethodArgumentResolverUnitTests {

	@Test
	public void buildsUpRequestParameters() {

		String basicString = String.format("page=%d&size=%d", PAGE_NUMBER, PAGE_SIZE);

		assertUriStringFor(REFERENCE_WITHOUT_SORT, basicString);
		assertUriStringFor(REFERENCE_WITH_SORT, basicString + "&sort=firstname,lastname,desc");
		assertUriStringFor(REFERENCE_WITH_SORT_FIELDS, basicString + "&sort=firstname,lastname,asc");
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
		assertThat(page).hasSize(1);
		assertThat(page.get(0)).isEqualTo("1");

		List<String> size = params.get("size");
		assertThat(size).hasSize(1);
		assertThat(size.get(0)).isEqualTo("20");
	}

	/**
	 * @see DATACMNS-335
	 */
	@Test
	public void preventsPageSizeFromExceedingMayValueIfConfiguredOnWrite() throws Exception {
		assertUriStringFor(new PageRequest(0, 200), "page=0&size=100");
	}

	/**
	 * @see DATACMNS-418
	 */
	@Test
	public void appendsTemplateVariablesCorrectly() {

		assertTemplateEnrichment("/foo", "{?page,size,sort}");
		assertTemplateEnrichment("/foo?bar=1", "{&page,size,sort}");
		assertTemplateEnrichment("/foo?page=1", "{&size,sort}");
		assertTemplateEnrichment("/foo?page=1&size=10", "{&sort}");
		assertTemplateEnrichment("/foo?page=1&sort=foo,asc", "{&size}");
		assertTemplateEnrichment("/foo?page=1&size=10&sort=foo,asc", "");
	}

	/**
	 * @see DATACMNS-418
	 */
	@Test
	public void returnsCustomizedTemplateVariables() {

		UriComponents uriComponents = UriComponentsBuilder.fromPath("/foo").build();

		HateoasPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setPageParameterName("foo");
		String variables = resolver.getPaginationTemplateVariables(null, uriComponents).toString();

		assertThat(variables).isEqualTo("{?foo,size,sort}");
	}

	/**
	 * @see DATACMNS-563
	 */
	@Test
	public void enablingOneIndexedParameterReturnsOneForFirstPage() {

		HateoasPageableHandlerMethodArgumentResolver resolver = getResolver();
		resolver.setOneIndexedParameters(true);

		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");

		resolver.enhance(builder, null, new PageRequest(0, 10));

		MultiValueMap<String, String> params = builder.build().getQueryParams();

		assertThat(params.containsKey(resolver.getPageParameterName())).isTrue();
		assertThat(params.getFirst(resolver.getPageParameterName())).isEqualTo("1");
	}

	@Override
	protected HateoasPageableHandlerMethodArgumentResolver getResolver() {

		HateoasPageableHandlerMethodArgumentResolver resolver = new HateoasPageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	protected void assertUriStringFor(Pageable pageable, String expected) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		getResolver().enhance(builder, parameter, pageable);

		assertThat(builder.build().toUriString()).endsWith(expected);
	}

	private void assertTemplateEnrichment(String baseUri, String expected) {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUri).build();

		HateoasPageableHandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.getPaginationTemplateVariables(null, uriComponents).toString()).isEqualTo(expected);
	}
}
