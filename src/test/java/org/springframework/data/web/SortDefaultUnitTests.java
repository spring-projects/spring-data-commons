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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/**
 * Unit tests for {@link SortDefault}.
 * 
 * @since 1.6
 * @author Oliver Gierke
 */
public abstract class SortDefaultUnitTests {

	static final String[] SORT_FIELDS = new String[] { "firstname", "lastname" };
	static final Direction SORT_DIRECTION = Direction.DESC;

	static final Sort SORT = new Sort(SORT_DIRECTION, SORT_FIELDS);

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Test
	public void supportsSortParameter() {
		assertThat(getResolver().supportsParameter(getParameterOfMethod("supportedMethod")), is(true));
	}

	@Test
	public void returnsNullForNoDefault() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("supportedMethod"), null);
	}

	@Test
	public void discoversSimpleDefault() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefault"), new Sort(Direction.ASC, SORT_FIELDS));
	}

	@Test
	public void discoversSimpleDefaultWithDirection() throws Exception {
		assertSupportedAndResolvedTo(getParameterOfMethod("simpleDefaultWithDirection"), SORT);
	}

	@Test
	public void rejectsNonSortParameter() {

		MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
		assertThat(getResolver().supportsParameter(parameter), is(false));
	}

	@Test
	public void rejectsDoubleAnnotatedMethod() throws Exception {

		MethodParameter parameter = getParameterOfMethod("invalid");

		HandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
		assertThat(resolver.supportsParameter(parameter), is(true));

		exception.expect(IllegalArgumentException.class);
		exception.expectMessage(SortDefault.class.getSimpleName());
		exception.expectMessage(SortDefaults.class.getSimpleName());
		exception.expectMessage(parameter.toString());

		resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null);
	}

	@Test
	public void discoversContaineredDefault() throws Exception {

		MethodParameter parameter = getParameterOfMethod("containeredDefault");
		Sort reference = new Sort("foo", "bar");

		assertSupportedAndResolvedTo(parameter, reference);
	}

	protected abstract HandlerMethodArgumentResolver getResolver();

	protected abstract Class<?> getControllerClass();

	private void assertSupportedAndResolvedTo(MethodParameter parameter, Sort sort) throws Exception {

		HandlerMethodArgumentResolver resolver = getResolver();
		assertThat(resolver.supportsParameter(parameter), is(true));
		assertThat(resolver.resolveArgument(parameter, null, TestUtils.getWebRequest(), null), is((Object) sort));
	}

	protected MethodParameter getParameterOfMethod(String name) {
		return getParameterOfMethod(getControllerClass(), name);
	}

	private static MethodParameter getParameterOfMethod(Class<?> controller, String name) {
		return TestUtils.getParameterOfMethod(controller, name, Sort.class);
	}
}
