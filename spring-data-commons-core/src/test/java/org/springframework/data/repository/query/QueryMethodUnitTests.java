/*
 * Copyright 2008-2011 the original author or authors.
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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class QueryMethodUnitTests {

	@Mock
	RepositoryMetadata metadata;

	@Test(expected = IllegalStateException.class)
	public void rejectsPagingMethodWithInvalidReturnType() throws Exception {

		Method method = SampleRepository.class.getMethod("pagingMethodWithInvalidReturnType", Pageable.class);
		new QueryMethod(method, metadata);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsPagingMethodWithoutPageable() throws Exception {
		Method method = SampleRepository.class.getMethod("pagingMethodWithoutPageable");
		new QueryMethod(method, metadata);
	}

	@Test
	public void setsUpSimpleQueryMethodCorrectly() throws Exception {
		Method method = SampleRepository.class.getMethod("findByUsername", String.class);
		new QueryMethod(method, metadata);
	}

	@Test
	public void considersIterableMethodForCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("sampleMethod");
		QueryMethod queryMethod = new QueryMethod(method, metadata);
		assertThat(queryMethod.isCollectionQuery(), is(true));
	}

	@Test
	public void doesNotConsiderPageMethodCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("anotherSampleMethod", Pageable.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata);
		assertThat(queryMethod.isPageQuery(), is(true));
		assertThat(queryMethod.isCollectionQuery(), is(false));
	}

	interface SampleRepository {
		String pagingMethodWithInvalidReturnType(Pageable pageable);

		Page<String> pagingMethodWithoutPageable();

		String findByUsername(String username);

		Iterable<String> sampleMethod();

		Page<String> anotherSampleMethod(Pageable pageable);
	}
}
