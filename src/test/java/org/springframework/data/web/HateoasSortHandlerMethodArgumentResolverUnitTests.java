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
import static org.springframework.data.domain.Sort.Direction.*;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for {@link HateoasSortHandlerMethodArgumentResolver}
 * 
 * @author Oliver Gierke
 */
public class HateoasSortHandlerMethodArgumentResolverUnitTests extends SortHandlerMethodArgumentResolverUnitTests {

	@Test
	public void buildsUpRequestParameters() {

		assertUriStringFor(SORT, "sort=firstname,lastname,desc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(DESC, "bar").and(new Sort(ASC, "foobar"))),
				"sort=foo,asc&sort=bar,desc&sort=foobar,asc");
		assertUriStringFor(new Sort(ASC, "foo").and(new Sort(ASC, "bar").and(new Sort(DESC, "foobar"))),
				"sort=foo,bar,asc&sort=foobar,desc");
	}

	private void assertUriStringFor(Sort sort, String expected) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/");
		MethodParameter parameter = getParameterOfMethod("supportedMethod");

		new HateoasSortHandlerMethodArgumentResolver().enhance(builder, parameter, sort);

		assertThat(builder.build().toUriString(), endsWith(expected));
	}
}
