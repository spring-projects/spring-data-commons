/*
 * Copyright 2016 the original author or authors.
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

import rx.Observable;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactivePagingAndSortingRepository;
import org.springframework.data.repository.reactive.RxJavaCrudRepository;
import org.springframework.data.repository.util.QueryExecutionConverters;

/**
 * Unit tests for {@link ConvertingMethodParameterRepositoryInformation}.
 *
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveRepositoryInformationUnitTests {

	static final Class<ReactiveJavaInterfaceWithGenerics> REPOSITORY = ReactiveJavaInterfaceWithGenerics.class;

	@Test
	public void discoversMethodWithoutComparingReturnType() throws Exception {

		Method method = RxJavaInterfaceWithGenerics.class.getMethod("deleteAll");
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(RxJavaInterfaceWithGenerics.class);
		DefaultRepositoryInformation information = new DefaultRepositoryInformation(metadata, REPOSITORY, null);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("deleteAll"));
	}

	@Test
	public void discoversMethodWithConvertibleArguments() throws Exception {

		DefaultConversionService conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);

		Method method = RxJavaInterfaceWithGenerics.class.getMethod("save", Observable.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(RxJavaInterfaceWithGenerics.class);
		DefaultRepositoryInformation information = new ReactiveRepositoryInformation(metadata, REPOSITORY, null,
				conversionService);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("save"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Publisher.class)));
	}

	@Test
	public void discoversMethodAssignableArguments() throws Exception {

		DefaultConversionService conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);

		Method method = ReactivePagingAndSortingRepository.class.getMethod("save", Publisher.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ReactiveJavaInterfaceWithGenerics.class);
		DefaultRepositoryInformation information = new ReactiveRepositoryInformation(metadata, REPOSITORY, null,
				conversionService);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("save"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Publisher.class)));
	}

	@Test
	public void discoversMethodExactIterableArguments() throws Exception {

		DefaultConversionService conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);

		Method method = ReactiveJavaInterfaceWithGenerics.class.getMethod("save", Iterable.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ReactiveJavaInterfaceWithGenerics.class);
		DefaultRepositoryInformation information = new ReactiveRepositoryInformation(metadata, REPOSITORY, null,
				conversionService);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("save"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Iterable.class)));
	}

	@Test
	public void discoversMethodExactObjectArguments() throws Exception {

		DefaultConversionService conversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(conversionService);

		Method method = ReactiveJavaInterfaceWithGenerics.class.getMethod("save", Object.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(ReactiveJavaInterfaceWithGenerics.class);
		DefaultRepositoryInformation information = new ReactiveRepositoryInformation(metadata, REPOSITORY, null,
				conversionService);

		Method reference = information.getTargetClassMethod(method);
		assertEquals(ReactiveCrudRepository.class, reference.getDeclaringClass());
		assertThat(reference.getName(), is("save"));
		assertThat(reference.getParameterTypes()[0], is(equalTo(Object.class)));
	}

	interface RxJavaInterfaceWithGenerics extends RxJavaCrudRepository<User, String> {}

	interface ReactiveJavaInterfaceWithGenerics extends ReactiveCrudRepository<User, String> {}

	static abstract class DummyGenericReactiveRepositorySupport<T, ID extends Serializable>
			implements ReactiveCrudRepository<T, ID> {

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
