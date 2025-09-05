/*
 * Copyright 2013-2025 the original author or authors.
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

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HateoasSortHandlerMethodArgumentResolver}
 *
 * @author Oliver Gierke
 * @author Julien Béti
 */
class HateoasSortHandlerMethodArgumentResolverUnitTests extends SortHandlerMethodArgumentResolverUnitTests {

	@Test
	void buildsUpRequestParameters() throws Exception {

		assertUriStringFor(SORT, "sort=firstname,lastname,desc");
		assertUriStringFor(Sort.by(ASC, "foo").and(Sort.by(DESC, "bar").and(Sort.by(ASC, "foobar"))),
				"sort=foo,asc&sort=bar,desc&sort=foobar,asc");
		assertUriStringFor(Sort.by(ASC, "foo").and(Sort.by(ASC, "bar").and(Sort.by(DESC, "foobar"))),
				"sort=foo,bar,asc&sort=foobar,desc");
	}

	@Test // DATACMNS-407
	void replacesExistingRequestParameters() throws Exception {
		assertUriStringFor(SORT, "/?sort=firstname,lastname,desc", "/?sort=foo,asc");
	}

	@Test // DATACMNS-418, GH-2531
	void returnCorrectTemplateVariables() {

		var uriComponents = UriComponentsBuilder.fromPath("/").build();

		var resolver = new HateoasSortHandlerMethodArgumentResolver();
		assertThat(resolver.getSortTemplateVariables(null, uriComponents).toString()).isEqualTo("{?sort*}");
	}

	private void assertUriStringFor(Sort sort, String expected) throws Exception {
		assertUriStringFor(sort, expected, "/");
	}

	private void assertUriStringFor(Sort sort, String expected, String baseUri) throws Exception {

		var builder = UriComponentsBuilder.fromUri(new URI(baseUri));
		var parameter = getParameterOfMethod("supportedMethod");

		new HateoasSortHandlerMethodArgumentResolver().enhance(builder, parameter, sort);

		assertThat(builder.build().toUriString()).endsWith(expected);
	}
}
