/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


/**
 * Unit test for {@link Parameters}.
 *
 * @author Oliver Gierke
 */
public class ParametersUnitTests {

	private Method valid;


	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		valid = SampleDao.class.getMethod("valid", String.class);
	}


	@Test
	public void checksValidMethodCorrectly() throws Exception {

		Method validWithPageable =
				SampleDao.class.getMethod("validWithPageable", String.class,
						Pageable.class);
		Method validWithSort =
				SampleDao.class.getMethod("validWithSort", String.class,
						Sort.class);

		new Parameters(valid);
		new Parameters(validWithPageable);
		new Parameters(validWithSort);
	}


	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidMethodWithParamMissing() throws Exception {

		Method method =
				SampleDao.class.getMethod("invalidParamMissing", String.class,
						String.class);
		new Parameters(method);
	}


	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMethod() throws Exception {

		new Parameters(null);
	}


	@Test
	public void detectsNamedParameterCorrectly() throws Exception {

		Parameters parameters =
				getParametersFor("validWithSort", String.class, Sort.class);

		Parameter parameter = parameters.getParameter(0);

		assertThat(parameter.isNamedParameter(), is(true));
		assertThat(parameter.getPlaceholder(), is(":username"));

		parameter = parameters.getParameter(1);

		assertThat(parameter.isNamedParameter(), is(false));
		assertThat(parameter.isSpecialParameter(), is(true));
	}


	@Test
	public void calculatesPlaceholderPositionCorrectly() throws Exception {

		Method method =
				SampleDao.class.getMethod("validWithSortFirst", Sort.class,
						String.class);

		Parameters parameters = new Parameters(method);
		assertThat(parameters.getBindableParameter(0).getIndex(), is(1));

		method =
				SampleDao.class.getMethod("validWithSortInBetween",
						String.class, Sort.class, String.class);

		parameters = new Parameters(method);

		assertThat(parameters.getBindableParameter(0).getIndex(), is(0));
		assertThat(parameters.getBindableParameter(1).getIndex(), is(2));
	}


	@Test
	public void detectsEmptyParameterListCorrectly() throws Exception {

		Parameters parameters = getParametersFor("emptyParameters");
		assertThat(parameters.hasParameterAt(0), is(false));
	}


	private Parameters getParametersFor(String methodName,
																			Class<?>... parameterTypes) throws SecurityException,
			NoSuchMethodException {

		Method method = SampleDao.class.getMethod(methodName, parameterTypes);

		return new Parameters(method);
	}

	static class User {

	}

	static interface SampleDao {

		User valid(@Param("username") String username);


		User invalidParamMissing(@Param("username") String username,
														 String lastname);


		User validWithPageable(@Param("username") String username,
													 Pageable pageable);


		User validWithSort(@Param("username") String username, Sort sort);


		User validWithSortFirst(Sort sort, String username);


		User validWithSortInBetween(String firstname, Sort sort, String lastname);


		User emptyParameters();

	}
}
