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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.*;

import java.net.URI;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HateoasSortHandlerMethodArgumentResolver}
 * 
 * @author Oliver Gierke
 */
public class HateoasSortHandlerMethodArgumentResolverUnitTests extends SortHandlerMethodArgumentResolverUnitTests {

	@Test
	public void buildsUpRequestParameters() throws Exception {

		assertUriStringFor(SORT, "sort=firstname,lastname,desc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(DESC, "bar").and(new Sort(ASC, "foobar"))),
				"sort=foo,asc&sort=bar,desc&sort=foobar,asc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(ASC, "bar").and(new Sort(DESC, "foobar"))),
				"sort=foo,bar,asc&sort=foobar,desc");
	}

	/**
	 * @see DATACMNS-407
	 */
	@Test
	public void replacesExistingRequestParameters() throws Exception {
		assertUriStringFor(SORT, "/?sort=firstname,lastname,desc", "/?sort=foo,asc");
	}

	/**
	 * @see DATACMNS-418
	 */
	@Test
	public void returnCorrectTemplateVariables() {

		UriComponents uriComponents = UriComponentsBuilder.fromPath("/").build();

		HateoasSortHandlerMethodArgumentResolver resolver = new HateoasSortHandlerMethodArgumentResolver();
		assertThat(resolver.getSortTemplateVariables(null, uriComponents).toString()).isEqualTo("{?sort}");
	}

	private void assertUriStringFor(Sort sort, String expected) throws Exception {
		assertUriStringFor(sort, expected, "/");
	}

	private void assertUriStringFor(Sort sort, String expected, String baseUri) throws Exception {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(new URI(baseUri));
		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		new HateoasSortHandlerMethodArgumentResolver().enhance(builder, parameter, sort);

		assertThat(builder.build().toUriString()).endsWith(expected);
	}
}
