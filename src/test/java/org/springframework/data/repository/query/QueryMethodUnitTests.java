/*
 * Copyright 2008-2015 the original author or authors.
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
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assume.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.core.SpringVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.util.Version;

/**
 * Unit tests for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class QueryMethodUnitTests {

	private static final Version SPRING_VERSION = Version.parse(SpringVersion.getVersion());
	private static final Version FOUR_DOT_TWO = new Version(4, 2);

	RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);
	ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	/**
	 * @see DATAJPA-59
	 */
	@Test(expected = IllegalStateException.class)
	public void rejectsPagingMethodWithInvalidReturnType() throws Exception {

		Method method = SampleRepository.class.getMethod("pagingMethodWithInvalidReturnType", Pageable.class);
		new QueryMethod(method, metadata, factory);
	}

	/**
	 * @see DATAJPA-59
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsPagingMethodWithoutPageable() throws Exception {
		Method method = SampleRepository.class.getMethod("pagingMethodWithoutPageable");
		new QueryMethod(method, metadata, factory);
	}

	/**
	 * @see DATACMNS-64
	 */
	@Test
	public void setsUpSimpleQueryMethodCorrectly() throws Exception {
		Method method = SampleRepository.class.getMethod("findByUsername", String.class);
		new QueryMethod(method, metadata, factory);
	}

	/**
	 * @see DATACMNS-61
	 */
	@Test
	public void considersIterableMethodForCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("sampleMethod");
		QueryMethod queryMethod = new QueryMethod(method, metadata, factory);
		assertThat(queryMethod.isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-67
	 */
	@Test
	public void doesNotConsiderPageMethodCollectionQuery() throws Exception {
		Method method = SampleRepository.class.getMethod("anotherSampleMethod", Pageable.class);
		QueryMethod queryMethod = new QueryMethod(method, metadata, factory);
		assertThat(queryMethod.isPageQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
	}

	/**
	 * @see DATACMNS-171
	 */
	@Test
	public void detectsAnEntityBeingReturned() throws Exception {

		Method method = SampleRepository.class.getMethod("returnsEntitySubclass");
		QueryMethod queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isQueryForEntity()).isTrue();
	}

	/**
	 * @see DATACMNS-171
	 */
	@Test
	public void detectsNonEntityBeingReturned() throws Exception {

		Method method = SampleRepository.class.getMethod("returnsProjection");
		QueryMethod queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isQueryForEntity()).isFalse();
	}

	/**
	 * @see DATACMNS-397
	 */
	@Test
	public void detectsSliceMethod() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("sliceOfUsers");
		QueryMethod queryMethod = new QueryMethod(method, repositoryMetadata, factory);

		assertThat(queryMethod.isSliceQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
		assertThat(queryMethod.isPageQuery()).isFalse();
	}

	/**
	 * @see DATACMNS-471
	 */
	@Test
	public void detectsCollectionMethodForArrayRetrunType() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("arrayOfUsers");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-650
	 */
	@Test
	public void considersMethodReturningAStreamStreaming() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("streaming");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isStreamQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-650
	 */
	@Test
	public void doesNotRejectStreamingForPagination() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("streaming", Pageable.class);

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isStreamQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-716
	 */
	@Test
	public void doesNotRejectCompletableFutureQueryForSingleEntity() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("returnsCompletableFutureForSingleEntity");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isFalse();
	}

	/**
	 * @see DATACMNS-716
	 */
	@Test
	public void doesNotRejectCompletableFutureQueryForEntityCollection() throws Exception {

		assumeThat(SPRING_VERSION.isGreaterThanOrEqualTo(FOUR_DOT_TWO), is(true));

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("returnsCompletableFutureForEntityCollection");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-716
	 */
	@Test
	public void doesNotRejectFutureQueryForSingleEntity() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("returnsFutureForSingleEntity");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isFalse();
	}

	/**
	 * @see DATACMNS-716
	 */
	@Test
	public void doesNotRejectFutureQueryForEntityCollection() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		Method method = SampleRepository.class.getMethod("returnsFutureForEntityCollection");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
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

		User[] arrayOfUsers();

		Stream<String> streaming();

		Stream<String> streaming(Pageable pageable);

		/**
		 * @see DATACMNS-716
		 */
		CompletableFuture<User> returnsCompletableFutureForSingleEntity();

		/**
		 * @see DATACMNS-716
		 */
		CompletableFuture<List<User>> returnsCompletableFutureForEntityCollection();

		/**
		 * @see DATACMNS-716
		 */
		Future<User> returnsFutureForSingleEntity();

		/**
		 * @see DATACMNS-716
		 */
		Future<List<User>> returnsFutureForEntityCollection();
	}

	class User {

	}

	class SpecialUser extends User {

	}
}
