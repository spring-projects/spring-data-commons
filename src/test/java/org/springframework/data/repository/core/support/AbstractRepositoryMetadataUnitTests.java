/*
 * Copyright 2011-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests for {@link AbstractRepositoryMetadata}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Fabian Buch
 */
class AbstractRepositoryMetadataUnitTests {

	@Test // DATACMNS-98
	void discoversSimpleReturnTypeCorrectly() throws Exception {

		RepositoryMetadata metadata = new DummyRepositoryMetadata(UserRepository.class);
		Method method = UserRepository.class.getMethod("findSingle");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-98
	void resolvesTypeParameterReturnType() throws Exception {
		RepositoryMetadata metadata = new DummyRepositoryMetadata(ConcreteRepository.class);
		Method method = ConcreteRepository.class.getMethod("intermediateMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-98
	void determinesReturnTypeFromPageable() throws Exception {

		RepositoryMetadata metadata = new DummyRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("findByFirstname", Pageable.class, String.class);
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-453
	void nonPageableRepository() {

		RepositoryMetadata metadata = new DummyRepositoryMetadata(UserRepository.class);
		assertThat(metadata.isPagingRepository()).isFalse();
	}

	@Test // DATACMNS-453
	void pageableRepository() {

		RepositoryMetadata metadata = new DummyRepositoryMetadata(PagedRepository.class);
		assertThat(metadata.isPagingRepository()).isTrue();
	}

	@Test // DATACMNS-98
	void determinesReturnTypeFromGenericType() throws Exception {
		RepositoryMetadata metadata = new DummyRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("someMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(GenericType.class);
	}

	@Test // DATACMNS-98
	void handlesGenericTypeInReturnedCollectionCorrectly() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DummyRepositoryMetadata(ExtendingRepository.class);
		Method method = ExtendingRepository.class.getMethod("anotherMethod");
		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(Map.class);
	}

	@Test // DATACMNS-471
	void detectsArrayReturnTypeCorrectly() throws Exception {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(PagedRepository.class);
		Method method = PagedRepository.class.getMethod("returnsArray");

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(User.class);
	}

	@Test // DATACMNS-1299
	void doesNotUnwrapCustomTypeImplementingIterable() throws Exception {

		RepositoryMetadata metadata = AbstractRepositoryMetadata.getMetadata(ContainerRepository.class);

		Method method = ContainerRepository.class.getMethod("someMethod");

		assertThat(metadata.getReturnedDomainClass(method)).isEqualTo(Container.class);
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

		GenericType<User> someMethod();

		List<Map<String, Object>> anotherMethod();
	}

	interface PagedRepository extends PagingAndSortingRepository<User, Long> {

		User[] returnsArray();
	}

	class GenericType<T> {

	}

	class DummyRepositoryMetadata extends AbstractRepositoryMetadata {

		DummyRepositoryMetadata(Class<?> repositoryInterface) {
			super(repositoryInterface);
		}

		@SuppressWarnings("unchecked")
		public Class<? extends Serializable> getIdType() {
			return (Class<? extends Serializable>) ResolvableType//
					.forClass(Repository.class, getRepositoryInterface()).getGeneric(1).resolve();
		}

		public Class<?> getDomainType() {
			return ResolvableType.forClass(Repository.class, getRepositoryInterface()).getGeneric(0).resolve();
		}
	}

	// DATACMNS-1299

	class Element {}

	abstract class Container implements Iterable<Element> {}

	interface ContainerRepository extends Repository<Container, Long> {
		Container someMethod();
	}
}
