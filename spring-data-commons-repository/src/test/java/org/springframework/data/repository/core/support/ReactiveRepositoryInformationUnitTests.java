/*
 * Copyright 2016-2025 the original author or authors.
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

import io.reactivex.rxjava3.core.Flowable;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/**
 * Unit tests for {@link ReactiveRepositoryInformation}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
class ReactiveRepositoryInformationUnitTests {

	static final Class<ReactiveJavaInterfaceWithGenerics> BASE_CLASS = ReactiveJavaInterfaceWithGenerics.class;

	@Test // DATACMNS-988
	void discoversRxJava3MethodWithoutComparingReturnType() throws Exception {

		var reference = extractTargetMethodFromRepository(RxJava3InterfaceWithGenerics.class, "deleteAll");

		assertThat(reference.getDeclaringClass()).isEqualTo(ReactiveCrudRepository.class);
		assertThat(reference.getName()).isEqualTo("deleteAll");
	}

	@Test // DATACMNS-988
	void discoversRxJava3MethodWithConvertibleArguments() throws Exception {

		var reference = extractTargetMethodFromRepository(RxJava3InterfaceWithGenerics.class, "saveAll", Flowable.class);

		assertThat(reference.getDeclaringClass()).isEqualTo(ReactiveCrudRepository.class);
		assertThat(reference.getName()).isEqualTo("saveAll");
		assertThat(reference.getParameterTypes()[0]).isEqualTo(Publisher.class);
	}

	@Test // DATACMNS-836
	void discoversMethodAssignableArguments() throws Exception {

		var reference = extractTargetMethodFromRepository(ReactiveCrudRepository.class, "saveAll", Publisher.class);

		assertThat(reference.getDeclaringClass()).isEqualTo(ReactiveCrudRepository.class);
		assertThat(reference.getName()).isEqualTo("saveAll");
		assertThat(reference.getParameterTypes()[0]).isEqualTo(Publisher.class);
	}

	@Test // DATACMNS-836
	void discoversMethodExactIterableArguments() throws Exception {

		var reference = extractTargetMethodFromRepository(ReactiveJavaInterfaceWithGenerics.class, "saveAll",
				Iterable.class);

		assertThat(reference.getDeclaringClass()).isEqualTo(ReactiveCrudRepository.class);
		assertThat(reference.getName()).isEqualTo("saveAll");
		assertThat(reference.getParameterTypes()[0]).isEqualTo(Iterable.class);
	}

	@Test // DATACMNS-836
	void discoversMethodExactObjectArguments() throws Exception {

		var reference = extractTargetMethodFromRepository(ReactiveJavaInterfaceWithGenerics.class, "save", Object.class);

		assertThat(reference.getDeclaringClass()).isEqualTo(ReactiveCrudRepository.class);
		assertThat(reference.getName()).isEqualTo("save");
		assertThat(reference.getParameterTypes()[0]).isEqualTo(Object.class);
	}

	@Test // DATACMNS-1023
	void usesCorrectSaveOverload() throws Exception {

		var reference = extractTargetMethodFromRepository(DummyRepository.class, "saveAll", Iterable.class);

		assertThat(reference).isEqualTo(ReactiveCrudRepository.class.getMethod("saveAll", Iterable.class));
	}

	private Method extractTargetMethodFromRepository(Class<?> repositoryType, String methodName, Class<?>... args)
			throws NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repositoryType);

		var composition = RepositoryComposition.of(RepositoryFragment.structural(BASE_CLASS))
				.withMethodLookup(MethodLookups.forReactiveTypes(metadata));

		return composition.findMethod(repositoryType.getMethod(methodName, args)).get();
	}

	interface RxJava3InterfaceWithGenerics extends RxJava3CrudRepository<User, String> {}

	interface ReactiveJavaInterfaceWithGenerics extends ReactiveCrudRepository<User, String> {}

	static abstract class DummyGenericReactiveRepositorySupport<T, ID> implements ReactiveCrudRepository<T, ID> {

	}

	interface DummyRepository extends ReactiveCrudRepository<User, Integer> {

		@Override
		<S extends User> Flux<S> saveAll(Iterable<S> entities);
	}

	static class User {

		String id;

		String getId() {
			return id;
		}

		void setId(String id) {
			this.id = id;
		}

	}
}
