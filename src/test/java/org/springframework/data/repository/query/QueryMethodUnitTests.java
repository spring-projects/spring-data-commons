/*
 * Copyright 2008-2023 the original author or authors.
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

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import org.springframework.data.domain.Window;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link QueryMethod}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Maciek Opała
 * @author Mark Paluch
 */
class QueryMethodUnitTests {

	RepositoryMetadata metadata = new DefaultRepositoryMetadata(SampleRepository.class);
	ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	@Test // DATAJPA-59
	void rejectsPagingMethodWithInvalidReturnType() throws Exception {

		var method = SampleRepository.class.getMethod("pagingMethodWithInvalidReturnType", Pageable.class);

		assertThatIllegalStateException().isThrownBy(() -> new QueryMethod(method, metadata, factory));
	}

	@Test // DATAJPA-59
	void rejectsPagingMethodWithoutPageable() throws Exception {
		var method = SampleRepository.class.getMethod("pagingMethodWithoutPageable");

		assertThatIllegalArgumentException().isThrownBy(() -> new QueryMethod(method, metadata, factory));
	}

	@Test // DATACMNS-64
	void setsUpSimpleQueryMethodCorrectly() throws Exception {
		var method = SampleRepository.class.getMethod("findByUsername", String.class);
		new QueryMethod(method, metadata, factory);
	}

	@Test // DATACMNS-61
	void considersIterableMethodForCollectionQuery() throws Exception {
		var method = SampleRepository.class.getMethod("sampleMethod");
		var queryMethod = new QueryMethod(method, metadata, factory);
		assertThat(queryMethod.isCollectionQuery()).isTrue();
	}

	@Test // DATACMNS-67
	void doesNotConsiderPageMethodCollectionQuery() throws Exception {
		var method = SampleRepository.class.getMethod("anotherSampleMethod", Pageable.class);
		var queryMethod = new QueryMethod(method, metadata, factory);
		assertThat(queryMethod.isPageQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
	}

	@Test // GH-2151
	void supportsImperativecursorQueries() throws Exception {
		var method = SampleRepository.class.getMethod("cursorWindow", ScrollPosition.class);
		var queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isPageQuery()).isFalse();
		assertThat(queryMethod.isScrollQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
	}

	@Test // GH-2151
	void supportsReactiveCursorQueries() throws Exception {
		var method = SampleRepository.class.getMethod("reactiveCursorWindow", ScrollPosition.class);
		var queryMethod = new QueryMethod(method, metadata, factory);
		assertThat(queryMethod.isPageQuery()).isFalse();

		assertThat(queryMethod.isScrollQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
	}

	@Test // GH-2151
	void rejectsInvalidReactiveCursorQueries() throws Exception {
		var method = SampleRepository.class.getMethod("invalidReactiveCursorWindow", ScrollPosition.class);

		assertThatIllegalStateException().isThrownBy(() -> new QueryMethod(method, metadata, factory));
	}

	@Test // GH-2151
	void rejectsCursorWindowMethodWithoutPageable() throws Exception {
		var method = SampleRepository.class.getMethod("cursorWindowWithoutScrollPosition");

		assertThatIllegalArgumentException().isThrownBy(() -> new QueryMethod(method, metadata, factory));
	}

	@Test // GH-2151
	void rejectsCursorWindowMethodWithInvalidReturnType() throws Exception {

		var method = SampleRepository.class.getMethod("cursorWindowMethodWithInvalidReturnType", ScrollPosition.class);

		assertThatIllegalStateException().isThrownBy(() -> new QueryMethod(method, metadata, factory));
	}

	@Test // DATACMNS-171
	void detectsAnEntityBeingReturned() throws Exception {

		var method = SampleRepository.class.getMethod("returnsEntitySubclass");
		var queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isQueryForEntity()).isTrue();
	}

	@Test // DATACMNS-171
	void detectsNonEntityBeingReturned() throws Exception {

		var method = SampleRepository.class.getMethod("returnsProjection");
		var queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isQueryForEntity()).isFalse();
	}

	@Test // DATACMNS-397
	void detectsSliceMethod() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("sliceOfUsers");
		var queryMethod = new QueryMethod(method, repositoryMetadata, factory);

		assertThat(queryMethod.isSliceQuery()).isTrue();
		assertThat(queryMethod.isCollectionQuery()).isFalse();
		assertThat(queryMethod.isPageQuery()).isFalse();
	}

	@Test // DATACMNS-471
	void detectsCollectionMethodForArrayRetrunType() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("arrayOfUsers");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	@Test // DATACMNS-650
	void considersMethodReturningAStreamStreaming() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("streaming");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isStreamQuery()).isTrue();
	}

	@Test // DATACMNS-650
	void doesNotRejectStreamingForPagination() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("streaming", Pageable.class);

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isStreamQuery()).isTrue();
	}

	@Test // DATACMNS-716
	void doesNotRejectCompletableFutureQueryForSingleEntity() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsCompletableFutureForSingleEntity");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isFalse();
	}

	@Test // DATACMNS-716
	void doesNotRejectCompletableFutureQueryForEntityCollection() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsCompletableFutureForEntityCollection");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	@Test // DATACMNS-716
	void doesNotRejectFutureQueryForSingleEntity() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsFutureForSingleEntity");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isFalse();
	}

	@Test // DATACMNS-716
	void doesNotRejectFutureQueryForEntityCollection() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsFutureForEntityCollection");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	void detectsCustomCollectionReturnType() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsSeq");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	void detectsWrapperWithinWrapper() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsFutureOfSeq");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	/**
	 * @see DATACMNS-940
	 */
	@Test
	void detectsSingleValueWrapperWithinWrapper() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsFutureOfOption");

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isFalse();
	}

	@Test // DATACMNS-1005
	void doesNotRejectSeqForPagination() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("returnsSeq", Pageable.class);

		assertThat(new QueryMethod(method, repositoryMetadata, factory).isCollectionQuery()).isTrue();
	}

	@Test // DATACMNS-1300
	void doesNotConsiderMethodForIterableAggregateACollectionQuery() throws Exception {

		var metadata = AbstractRepositoryMetadata.getMetadata(ContainerRepository.class);
		var method = ContainerRepository.class.getMethod("someMethod");

		assertThat(new QueryMethod(method, metadata, factory).isCollectionQuery()).isFalse();
	}

	@Test // DATACMNS-1762
	void detectsReactiveSliceQuery() throws Exception {

		RepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(SampleRepository.class);
		var method = SampleRepository.class.getMethod("reactiveSlice");

		var queryMethod = new QueryMethod(method, repositoryMetadata, factory);
		var returnedType = queryMethod.getResultProcessor().getReturnedType();
		assertThat(queryMethod.isSliceQuery()).isTrue();
		assertThat(returnedType.getTypeToRead()).isEqualTo(User.class);
		assertThat(returnedType.getDomainType()).isEqualTo(User.class);
	}

	@Test // #1817
	void considersEclipseCollectionCollectionQuery() throws Exception {

		var method = SampleRepository.class.getMethod("returnsEclipseCollection");
		var queryMethod = new QueryMethod(method, metadata, factory);

		assertThat(queryMethod.isCollectionQuery()).isTrue();
	}

	@TestFactory // GH-2869
	Stream<DynamicTest> doesNotConsiderQueryMethodReturningAggregateImplementingStreamableACollectionQuery()
			throws Exception {

		var metadata = AbstractRepositoryMetadata.getMetadata(StreamableAggregateRepository.class);
		var stream = Stream.of(
				Map.entry("findBy", false),
				Map.entry("findSubTypeBy", false),
				Map.entry("findAllBy", true),
				Map.entry("findOptionalBy", false));

		return DynamicTest.stream(stream, //
				it -> it.getKey() + " considered collection query -> " + it.getValue(), //
				it -> {

					var method = StreamableAggregateRepository.class.getMethod(it.getKey());
					var queryMethod = new QueryMethod(method, metadata, factory);

					assertThat(queryMethod.isCollectionQuery()).isEqualTo(it.getValue());
				});
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

		// DATACMNS-716
		CompletableFuture<User> returnsCompletableFutureForSingleEntity();

		// DATACMNS-716
		CompletableFuture<List<User>> returnsCompletableFutureForEntityCollection();

		// DATACMNS-716
		Future<User> returnsFutureForSingleEntity();

		// DATACMNS-716
		Future<List<User>> returnsFutureForEntityCollection();

		Seq<User> returnsSeq();

		// DATACMNS-1005
		Seq<User> returnsSeq(Pageable pageable);

		Future<Seq<User>> returnsFutureOfSeq();

		Future<Option<User>> returnsFutureOfOption();

		Mono<Slice<User>> reactiveSlice();

		ImmutableList<User> returnsEclipseCollection();

		Window<User> cursorWindow(ScrollPosition cursorRequest);

		Mono<Window<User>> reactiveCursorWindow(ScrollPosition cursorRequest);

		Flux<Window<User>> invalidReactiveCursorWindow(ScrollPosition cursorRequest);

		Page<User> cursorWindowMethodWithInvalidReturnType(ScrollPosition cursorRequest);

		Window<User> cursorWindowWithoutScrollPosition();
	}

	class User {

	}

	class SpecialUser extends User {

	}

	// DATACMNS-1300

	class Element {}

	abstract class Container implements Iterable<Element> {}

	interface ContainerRepository extends Repository<Container, Long> {
		Container someMethod();
	}

	// GH-2869

	static abstract class StreamableAggregate implements Streamable<Object> {}

	interface StreamableAggregateRepository extends Repository<StreamableAggregate, Object> {

		StreamableAggregate findBy();

		StreamableAggregateSubType findSubTypeBy();

		Optional<StreamableAggregate> findOptionalBy();

		Streamable<StreamableAggregate> findAllBy();
	}

	static abstract class StreamableAggregateSubType extends StreamableAggregate {}
}
