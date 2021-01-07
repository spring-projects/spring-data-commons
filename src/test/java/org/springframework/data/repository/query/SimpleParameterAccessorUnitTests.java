/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link ParametersParameterAccessor}.
 *
 * @author Oliver Gierke
 */
class SimpleParameterAccessorUnitTests {

	Parameters<?, ?> parameters, sortParameters, pageableParameters;

	@BeforeEach
	void setUp() throws SecurityException, NoSuchMethodException {

		parameters = new DefaultParameters(Sample.class.getMethod("sample", String.class));
		sortParameters = new DefaultParameters(Sample.class.getMethod("sample1", String.class, Sort.class));
		pageableParameters = new DefaultParameters(Sample.class.getMethod("sample2", String.class, Pageable.class));
	}

	@Test
	void testname() throws Exception {
		new ParametersParameterAccessor(parameters, new Object[] { "test" });
	}

	@Test
	void rejectsNullParameters() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametersParameterAccessor(null, new Object[0]));
	}

	@Test
	void rejectsNullValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametersParameterAccessor(parameters, null));
	}

	@Test
	void rejectsTooLittleNumberOfArguments() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ParametersParameterAccessor(parameters, new Object[0]));
	}

	@Test
	void rejectsTooManyArguments() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ParametersParameterAccessor(parameters, new Object[] { "test", "test" }));
	}

	@Test
	void returnsNullForPageableAndSortIfNoneAvailable() throws Exception {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { "test" });

		assertThat(accessor.getPageable().isPaged()).isFalse();
		assertThat(accessor.getSort().isSorted()).isFalse();
	}

	@Test
	void returnsSortIfAvailable() {

		Sort sort = Sort.by("foo");
		ParameterAccessor accessor = new ParametersParameterAccessor(sortParameters, new Object[] { "test", sort });

		assertThat(accessor.getSort()).isEqualTo(sort);
		assertThat(accessor.getPageable().isPaged()).isFalse();
	}

	@Test
	void returnsPageableIfAvailable() {

		Pageable pageable = PageRequest.of(0, 10);
		ParameterAccessor accessor = new ParametersParameterAccessor(pageableParameters, new Object[] { "test", pageable });

		assertThat(accessor.getPageable()).isEqualTo(pageable);
		assertThat(accessor.getSort().isSorted()).isFalse();
	}

	@Test
	void returnsSortFromPageableIfAvailable() throws Exception {

		Sort sort = Sort.by("foo");
		Pageable pageable = PageRequest.of(0, 10, sort);
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
