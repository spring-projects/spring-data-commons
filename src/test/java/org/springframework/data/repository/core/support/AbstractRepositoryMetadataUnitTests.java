/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link AbstractRepositoryMetadata}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Fabian Buch
 * @author Alessandro Nistico
 */
class AbstractRepositoryMetadataUnitTests {

	@Test // DATACMNS-98
	void discoversSimpleReturnTypeCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		Method method = UserRepository.class.getMethod("findSingle");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-98
	void resolvesTypeParameterReturnType() throws Exception {
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ConcreteRepository.class);
		Method method = ConcreteRepository.class.getMethod("intermediateMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // GH-3270
	void detectsProjectionTypeCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("findByFirstname", Pageable.class, String.class, Class.class);

		ResolvableType resolvableType = metadata.getReturnedDomainTypeInformation(method).toResolvableType();
		assertThat(resolvableType.getType()).hasToString("T");
	}

	@Test // DATACMNS-98
	void determinesReturnTypeFromPageable() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("findByFirstname", Pageable.class, String.class);
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-453
	void nonPageableRepository() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		assertThat(metadata.isPagingRepository()).isFalse();
	}

	@Test // DATACMNS-453
	void pageableRepository() {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(PagedRepository.class);
		assertThat(metadata.isPagingRepository()).isTrue();
	}

	@Test // DATACMNS-98
	void determinesReturnTypeFromGenericType() throws Exception {
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("someMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(GenericType.class);
	}

	@Test // DATACMNS-98
	void handlesGenericTypeInReturnedCollectionCorrectly() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("anotherMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(Map.class);
	}

	@Test // DATACMNS-471
	void detectsArrayReturnTypeCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(CompletePageableAndSortingRepository.class);
		var method = PagedRepository.class.getMethod("returnsArray");

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-1299
	void doesNotUnwrapCustomTypeImplementingIterable() throws Exception {

		var metadata = AbstractRepositoryMetadata.getMetadata(ContainerRepository.class);

		var method = ContainerRepository.class.getMethod("someMethod");

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(Container.class);
	}

	@TestFactory // GH-2869
	Stream<DynamicTest> detectsReturnTypesForStreamableAggregates() throws Exception {

		var metadata = AbstractRepositoryMetadata.getMetadata(StreamableAggregateRepository.class);
		var methods = Stream.of(
				Map.entry("findBy", StreamableAggregate.class),
				Map.entry("findSubTypeBy", StreamableAggregateSubType.class),
				Map.entry("findAllBy", StreamableAggregate.class),
				Map.entry("findOptional", StreamableAggregate.class));

		return DynamicTest.stream(methods, //
				it -> it.getKey() + "'s returned domain class is " + it.getValue(), //
				it -> {

					var method = StreamableAggregateRepository.class.getMethod(it.getKey());
					assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(it.getValue());
				});
	}

	interface UserRepository extends Repository<User, Long> {

		User findSingle();
	}

	interface IntermediateRepository<T> extends Repository<T, Long> {

		List<T> intermediateMethod();
	}

	interface ConcreteRepository extends IntermediateRepository<User> {

	}

	interface ExtendingRepository extends Serializable, UserRepository {

		Page<User> findByFirstname(Pageable pageable, String firstname);

		<T> Page<T> findByFirstname(Pageable pageable, String firstname, Class<T> projectionType);

		GenericType<User> someMethod();

		List<Map<String, Object>> anotherMethod();
	}

	interface PagedRepository extends PagingAndSortingRepository<User, Long> {

		User[] returnsArray();
	}

	class GenericType<T> {

	}

	// DATACMNS-1299

	class Element {}

	abstract class Container implements Iterable<Element> {}

	interface ContainerRepository extends Repository<Container, Long> {
		Container someMethod();
	}

	interface CompletePageableAndSortingRepository extends PagingAndSortingRepository<Container, Long> {}

	// GH-2869

	static abstract class StreamableAggregate implements Streamable<Object> {}

	interface StreamableAggregateRepository extends Repository<StreamableAggregate, Object> {

		StreamableAggregate findBy();

		StreamableAggregateSubType findSubTypeBy();

		Streamable<StreamableAggregate> findAllBy();

		Optional<StreamableAggregate> findOptional();
	}

	static abstract class StreamableAggregateSubType extends StreamableAggregate {}
}
