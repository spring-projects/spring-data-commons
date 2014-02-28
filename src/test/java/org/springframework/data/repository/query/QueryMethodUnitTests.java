/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit tests for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
public class QueryMethodUnitTests {

	RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);

	/**
	 * @see DATAJPA-59
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsPagingMethodWithInvalidReturnType() throws Exception {

		Method method = SampleRepository.class.getMethod("pagingMethodWithInvalidReturnType", Pageable.class);
		new QueryMethod(method, metadata);
	}

	/**
	 * @see DATAJPA-59
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsPagingMethodWithoutPageable() throws Exception {
		Method method = SampleRepository.class.getMethod("pagingMethodWithoutPageable");
		new QueryMethod(method, metadata);
	}

	/**
	 * @see DATACMNS-64
	 */
	@Test
	public void setsUpSimpleQueryMethodCorrectly() throws Exception {
		Method method = SampleRepository.class.getMethod("findByUsername", String.class);
		new QueryMethod(method, metadata);
	}

	/**
	 * @see DATACMNS-61
	 */
	@Test
	public void considersIterableMethodForCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("sampleMethod");
		QueryMethod queryMethod = new QueryMethod(method, metadata);
		assertThat(queryMethod.isCollectionQuery(), is(true));
	}

	/**
	 * @see DATACMNS-67
	 */
	@Test
	public void doesNotConsiderPageMethodCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("anotherSampleMethod", Pageable.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata);
		assertThat(queryMethod.isPageQuery(), is(true));
		assertThat(queryMethod.isCollectionQuery(), is(false));
	}

	/**
	 * @see DATACMNS-171
	 */
	@Test
	public void detectsAnEntityBeingReturned() throws Exception {

		Method method = SampleRepository.class.getMethod("returnsEntitySubclass");
		QueryMethod queryMethod = new QueryMethod(method, metadata);

		assertThat(queryMethod.isQueryForEntity(), is(true));
	}

	/**
	 * @see DATACMNS-171
	 */
	@Test
	public void detectsNonEntityBeingReturned() throws Exception {

		Method method = SampleRepository.class.getMethod("returnsProjection");
		QueryMethod queryMethod = new QueryMethod(method, metadata);

		assertThat(queryMethod.isQueryForEntity(), is(false));
	}

	/**
	 * @see DATACMNS-397
	 */
	@Test
	public void detectsSliceMethod() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("sliceOfUsers");
		QueryMethod queryMethod = new QueryMethod(method, repositoryMetadata);

		assertThat(queryMethod.isSliceQuery(), is(true));
		assertThat(queryMethod.isCollectionQuery(), is(false));
		assertThat(queryMethod.isPageQuery(), is(false));
	}

	interface SampleRepository extends Repository<User, Serializable> {

		String pagingMethodWithInvalidReturnType(Pageable pageable);

		Page<String> pagingMethodWithoutPageable();

		String findByUsername(String username);

		Iterable<String> sampleMethod();

		Page<String> anotherSampleMethod(Pageable pageable);

		SpecialUser returnsEntitySubclass();

		Integer returnsProjection();

		Slice<User> sliceOfUsers();
	}

	class User {

	}

	class SpecialUser extends User {

	}
}
