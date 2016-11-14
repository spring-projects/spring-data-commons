/*
 * Copyright 2008-2013 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link ParametersParameterAccessor}.
 * 
 * @author Oliver Gierke
 */
public class SimpleParameterAccessorUnitTests {

	Parameters<?, ?> parameters, sortParameters, pageableParameters;

	@Before
	public void setUp() throws SecurityException, NoSuchMethodException {

		parameters = new DefaultParameters(Sample.class.getMethod("sample", String.class));
		sortParameters = new DefaultParameters(Sample.class.getMethod("sample1", String.class, Sort.class));
		pageableParameters = new DefaultParameters(Sample.class.getMethod("sample2", String.class, Pageable.class));
	}

	@Test
	public void testname() throws Exception {

		new ParametersParameterAccessor(parameters, new Object[] { "test" });
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullParameters() throws Exception {

		new ParametersParameterAccessor(null, new Object[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullValues() throws Exception {

		new ParametersParameterAccessor(parameters, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsTooLittleNumberOfArguments() throws Exception {

		new ParametersParameterAccessor(parameters, new Object[0]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsTooManyArguments() throws Exception {

		new ParametersParameterAccessor(parameters, new Object[] { "test", "test" });
	}

	@Test
	public void returnsNullForPageableAndSortIfNoneAvailable() throws Exception {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { "test" });
		assertThat(accessor.getPageable()).isNull();
		assertThat(accessor.getSort()).isNull();
	}

	@Test
	public void returnsSortIfAvailable() {

		Sort sort = new Sort("foo");
		ParameterAccessor accessor = new ParametersParameterAccessor(sortParameters, new Object[] { "test", sort });
		assertThat(accessor.getSort()).isEqualTo(sort);
		assertThat(accessor.getPageable()).isNull();
	}

	@Test
	public void returnsPageableIfAvailable() {

		Pageable pageable = new PageRequest(0, 10);
		ParameterAccessor accessor = new ParametersParameterAccessor(pageableParameters, new Object[] { "test", pageable });
		assertThat(accessor.getPageable()).isEqualTo(pageable);
		assertThat(accessor.getSort()).isNull();
	}

	@Test
	public void returnsSortFromPageableIfAvailable() throws Exception {

		Sort sort = new Sort("foo");
		Pageable pageable = new PageRequest(0, 10, sort);
		ParameterAccessor accessor = new ParametersParameterAccessor(pageableParameters, new Object[] { "test", pageable });
		assertThat(accessor.getPageable()).isEqualTo(pageable);
		assertThat(accessor.getSort()).isEqualTo(sort);
	}

	interface Sample {

		void sample(String firstname);

		void sample1(String firstname, Sort sort);

		void sample2(String firstname, Pageable pageable);
	}
}
