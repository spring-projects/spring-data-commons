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

import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.server.mvc.UriComponentsContributor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HateoasPageableHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 */
class HateoasPageableHandlerMethodArgumentResolverUnitTests
		extends PageableHandlerMethodArgumentResolverUnitTests {

	@Test
	void buildsUpRequestParameters() {

		var basicString = String.format("page=%d&size=%d", PAGE_NUMBER, PAGE_SIZE);

		assertUriStringFor(REFERENCE_WITHOUT_SORT, basicString);
		assertUriStringFor(REFERENCE_WITH_SORT, basicString + "&sort=firstname,lastname,desc");
		assertUriStringFor(REFERENCE_WITH_SORT_FIELDS, basicString + "&sort=firstname,lastname,asc");
	}

	@Test // DATACMNS-343
	void replacesExistingPaginationInformation() throws Exception {

		var parameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
		UriComponentsContributor resolver = new HateoasPageableHandlerMethodArgumentResolver();
		var builder = UriComponentsBuilder.fromHttpUrl("http://localhost:8080?page=0&size=10");
		resolver.enhance(builder, parameter, PageRequest.of(1, 20));

		var params = builder.build().getQueryParams();

		var page = params.get("page");
		assertThat(page).hasSize(1);
		assertThat(page.get(0)).isEqualTo("1");

		var size = params.get("size");
		assertThat(size).hasSize(1);
		assertThat(size.get(0)).isEqualTo("20");
	}

	@Test // DATACMNS-335
	void preventsPageSizeFromExceedingMayValueIfConfiguredOnWrite() throws Exception {
		assertUriStringFor(PageRequest.of(0, 200), "page=0&size=100");
	}

	@Test // DATACMNS-418
	void appendsTemplateVariablesCorrectly() {

		assertTemplateEnrichment("/foo", "{?page,size,sort}");
		assertTemplateEnrichment("/foo?bar=1", "{&page,size,sort}");
		assertTemplateEnrichment("/foo?page=1", "{&size,sort}");
		assertTemplateEnrichment("/foo?page=1&size=10", "{&sort}");
		assertTemplateEnrichment("/foo?page=1&sort=foo,asc", "{&size}");
		assertTemplateEnrichment("/foo?page=1&size=10&sort=foo,asc", "");
	}

	@Test // DATACMNS-418
	void returnsCustomizedTemplateVariables() {

		var uriComponents = UriComponentsBuilder.fromPath("/foo").build();

		var resolver = getResolver();
		resolver.setPageParameterName("foo");
		var variables = resolver.getPaginationTemplateVariables(null, uriComponents).toString();

		assertThat(variables).isEqualTo("{?foo,size,sort}");
	}

	@Test // DATACMNS-563
	void enablingOneIndexedParameterReturnsOneForFirstPage() {

		var resolver = getResolver();
		resolver.setOneIndexedParameters(true);

		var builder = UriComponentsBuilder.fromPath("/");

		resolver.enhance(builder, null, PageRequest.of(0, 10));

		var params = builder.build().getQueryParams();

		assertThat(params.containsKey(resolver.getPageParameterName())).isTrue();
		assertThat(params.getFirst(resolver.getPageParameterName())).isEqualTo("1");
	}

	@Test // DATACMNS-1455
	void enhancesUnpaged() {

		var builder = UriComponentsBuilder.fromPath("/");

		getResolver().enhance(builder, null, Pageable.unpaged());

		assertThat(builder).isEqualTo(builder);
	}

	@Override
	protected HateoasPageableHandlerMethodArgumentResolver getResolver() {

		var resolver = new HateoasPageableHandlerMethodArgumentResolver();
		resolver.setMaxPageSize(100);
		return resolver;
	}

	protected void assertUriStringFor(Pageable pageable, String expected) {

		var builder = UriComponentsBuilder.fromPath("/");
		var parameter = getParameterOfMethod("supportedMethod");

		getResolver().enhance(builder, parameter, pageable);

		assertThat(builder.build().toUriString()).endsWith(expected);
	}

	private void assertTemplateEnrichment(String baseUri, String expected) {

		var uriComponents = UriComponentsBuilder.fromUriString(baseUri).build();

		var resolver = getResolver();
		assertThat(resolver.getPaginationTemplateVariables(null, uriComponents).toString()).isEqualTo(expected);
	}
}
