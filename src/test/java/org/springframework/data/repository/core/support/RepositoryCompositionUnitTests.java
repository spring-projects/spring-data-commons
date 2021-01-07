/*
 * Copyright 2017-2021 the original author or authors.
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
import static org.mockito.Mockito.*;

import lombok.Data;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Example;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link RepositoryComposition}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class RepositoryCompositionUnitTests {

	@Mock QueryByExampleExecutor<Person> queryByExampleExecutor;
	@Mock PersonRepository backingRepo;

	RepositoryComposition repositoryComposition;

	@BeforeEach
	@SuppressWarnings("rawtypes")
	void before() {

		RepositoryInformation repositoryInformation = new DefaultRepositoryInformation(
				new DefaultRepositoryMetadata(PersonRepository.class), backingRepo.getClass(), RepositoryComposition.empty());

		RepositoryFragment<QueryByExampleExecutor> mixin = RepositoryFragment.implemented(QueryByExampleExecutor.class,
				queryByExampleExecutor);

		RepositoryFragment<PersonRepository> base = RepositoryFragment.implemented(backingRepo);

		repositoryComposition = RepositoryComposition.of(RepositoryFragments.of(mixin, base))
				.withMethodLookup(MethodLookups.forRepositoryTypes(repositoryInformation));
	}

	@Test // DATACMNS-102
	void shouldReportIfEmpty() {

		assertThat(RepositoryComposition.empty().isEmpty()).isTrue();
		assertThat(repositoryComposition.isEmpty()).isFalse();
	}

	@Test // DATACMNS-102
	void shouldCallSaveOnBackingRepo() throws Throwable {

		Method save = ReflectionUtils.findMethod(PersonRepository.class, "save", Person.class);

		Method method = repositoryComposition.findMethod(save).get();

		Person person = new Person();
		repositoryComposition.invoke(method, person);

		verify(backingRepo).save(person);
	}

	@Test // DATACMNS-102
	void shouldCallObjectSaveOnBackingRepo() throws Throwable {

		Method save = ReflectionUtils.findMethod(PersonRepository.class, "save", Object.class);

		Method method = repositoryComposition.findMethod(save).get();

		Person person = new Person();
		repositoryComposition.invoke(method, person);

		verify(backingRepo).save((Object) person);
	}

	@Test // DATACMNS-102
	void shouldCallFindOneOnMixin() throws Throwable {

		Method findOne = ReflectionUtils.findMethod(PersonRepository.class, "findOne", Example.class);

		Method method = repositoryComposition.findMethod(findOne).get();

		Person person = new Person();
		Example<Person> example = Example.of(person);

		repositoryComposition.invoke(method, example);

		verify(queryByExampleExecutor).findOne(example);
	}

	@Test // DATACMNS-102
	void shouldCallMethodsInOrder() throws Throwable {

		RepositoryInformation repositoryInformation = new DefaultRepositoryInformation(
				new DefaultRepositoryMetadata(OrderedRepository.class), OrderedRepository.class, RepositoryComposition.empty());

		RepositoryFragment<?> foo = RepositoryFragment.implemented(FooMixinImpl.INSTANCE);
		RepositoryFragment<?> bar = RepositoryFragment.implemented(BarMixinImpl.INSTANCE);

		RepositoryComposition fooBar = RepositoryComposition.of(RepositoryFragments.of(foo, bar))
				.withMethodLookup(MethodLookups.forRepositoryTypes(repositoryInformation));

		RepositoryComposition barFoo = RepositoryComposition.of(RepositoryFragments.of(bar, foo))
				.withMethodLookup(MethodLookups.forRepositoryTypes(repositoryInformation));

		Method getString = ReflectionUtils.findMethod(OrderedRepository.class, "getString");

		assertThat(fooBar.invoke(fooBar.findMethod(getString).get())).isEqualTo("foo");

		assertThat(barFoo.invoke(barFoo.findMethod(getString).get())).isEqualTo("bar");
	}

	@Test // DATACMNS-102
	void shouldValidateStructuralFragments() {

		RepositoryComposition mixed = RepositoryComposition.of(RepositoryFragment.structural(QueryByExampleExecutor.class),
				RepositoryFragment.implemented(backingRepo));

		assertThatIllegalStateException() //
				.isThrownBy(mixed::validateImplementation) //
				.withMessageContaining(
						"Fragment org.springframework.data.repository.query.QueryByExampleExecutor has no implementation.");
	}

	@Test // DATACMNS-102
	void shouldValidateImplementationFragments() {

		RepositoryComposition mixed = RepositoryComposition.of(RepositoryFragment.implemented(backingRepo));

		mixed.validateImplementation();
	}

	@Test // DATACMNS-102
	@SuppressWarnings("rawtypes")
	void shouldAppendCorrectly() {

		RepositoryFragment<PersonRepository> initial = RepositoryFragment.implemented(backingRepo);
		RepositoryFragment<QueryByExampleExecutor> structural = RepositoryFragment.structural(QueryByExampleExecutor.class);

		assertThat(RepositoryComposition.of(initial).append(structural).getFragments()).containsSequence(initial,
				structural);
		assertThat(RepositoryComposition.of(initial).append(RepositoryFragments.of(structural)).getFragments())
				.containsSequence(initial, structural);
	}

	interface PersonRepository extends Repository<Person, String>, QueryByExampleExecutor<Person> {

		Person save(Person entity);

		Person save(Object entity);

		Person findOne(Person entity);
	}

	@Data
	static class Person {

		@Id String id;
	}

	@Data
	static class Contact {

		@Id String id;
	}

	interface OrderedRepository extends Repository<Person, String>, FooMixin, BarMixin {

	}

	interface FooMixin {

		String getString();
	}

	enum FooMixinImpl implements FooMixin {
		INSTANCE;

		@Override
		public String getString() {
			return "foo";
		}
	}

	interface BarMixin {

		String getString();
	}

	enum BarMixinImpl implements BarMixin {
		INSTANCE;

		@Override
		public String getString() {
			return "bar";
		}
	}
}
