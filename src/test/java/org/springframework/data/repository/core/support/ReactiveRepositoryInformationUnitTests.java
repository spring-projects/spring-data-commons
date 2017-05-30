/*
 * Copyright 2016-2017 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;
import rx.Observable;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.data.repository.reactive.RxJava2CrudRepository;

/**
 * Unit tests for {@link ReactiveRepositoryInformation}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveRepositoryInformationUnitTests {

	static final Class<ReactiveJavaInterfaceWithGenerics> BASE_CLASS = ReactiveJavaInterfaceWithGenerics.class;

	@Test // DATACMNS-836
	public void discoversRxJava1MethodWithoutComparingReturnType() throws Exception {

		Method reference = extractTargetMethodFromRepository(RxJava1InterfaceWithGenerics.class, "deleteAll");

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("deleteAll"));
	}

	@Test // DATACMNS-836
	public void discoversRxJava1MethodWithConvertibleArguments() throws Exception {

		Method reference = extractTargetMethodFromRepository(RxJava1InterfaceWithGenerics.class, "saveAll",
				Observable.class);

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("saveAll"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Publisher.class)));
	}

	@Test // DATACMNS-988
	public void discoversRxJava2MethodWithoutComparingReturnType() throws Exception {

		Method reference = extractTargetMethodFromRepository(RxJava2InterfaceWithGenerics.class, "deleteAll");

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("deleteAll"));
	}

	@Test // DATACMNS-988
	public void discoversRxJava2MethodWithConvertibleArguments() throws Exception {

		Method reference = extractTargetMethodFromRepository(RxJava2InterfaceWithGenerics.class, "saveAll", Flowable.class);

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("saveAll"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Publisher.class)));
	}

	@Test // DATACMNS-836
	public void discoversMethodAssignableArguments() throws Exception {

		Method reference = extractTargetMethodFromRepository(ReactiveSortingRepository.class, "saveAll", Publisher.class);

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("saveAll"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Publisher.class)));
	}

	@Test // DATACMNS-836
	public void discoversMethodExactIterableArguments() throws Exception {

		Method reference = extractTargetMethodFromRepository(ReactiveJavaInterfaceWithGenerics.class, "saveAll",
				Iterable.class);

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("saveAll"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Iterable.class)));
	}

	@Test // DATACMNS-836
	public void discoversMethodExactObjectArguments() throws Exception {

		Method reference = extractTargetMethodFromRepository(ReactiveJavaInterfaceWithGenerics.class, "save", Object.class);

		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("save"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Object.class)));
	}

	@Test // DATACMNS-1023
	public void usesCorrectSaveOverload() throws Exception {

		Method reference = extractTargetMethodFromRepository(DummyRepository.class, "saveAll", Iterable.class);

		assertThat(reference, is(ReactiveCrudRepository.class.getMethod("saveAll", Iterable.class)));
	}

	private Method extractTargetMethodFromRepository(Class<?> repositoryType, String methodName, Class<?>... args)
			throws NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryType);

		RepositoryComposition composition = RepositoryComposition.of(RepositoryFragment.structural(BASE_CLASS))
				.withMethodLookup(MethodLookups.forReactiveTypes(metadata));

		return composition.findMethod(repositoryType.getMethod(methodName, args)).get();
	}

	interface RxJava1InterfaceWithGenerics extends Repository<User, String> {

		Observable<User> saveAll(Observable<User> entities);

		Completable deleteAll();
	}

	interface RxJava2InterfaceWithGenerics extends RxJava2CrudRepository<User, String> {}

	interface ReactiveJavaInterfaceWithGenerics extends ReactiveCrudRepository<User, String> {}

	static abstract class DummyGenericReactiveRepositorySupport<T, ID> implements ReactiveCrudRepository<T, ID> {

	}

	interface DummyRepository extends ReactiveCrudRepository<User, Integer> {

		@Override
		<S extends User> Flux<S> saveAll(Iterable<S> entities);
	}

	static class User {

		String id;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

	}
}
