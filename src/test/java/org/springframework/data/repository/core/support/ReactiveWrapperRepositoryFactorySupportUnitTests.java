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

import static org.mockito.Mockito.*;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import reactor.core.publisher.Mono;
import rx.Single;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

/**
 * Unit tests for {@link RepositoryFactorySupport} using reactive wrapper types.
 * 
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ReactiveWrapperRepositoryFactorySupportUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	DummyRepositoryFactory factory;

	@Mock ReactiveSortingRepository<Object, Serializable> backingRepo;
	@Mock ObjectRepositoryCustom customImplementation;

	@Before
	public void setUp() {
		factory = new DummyRepositoryFactory(backingRepo);
	}

	@Test // DATACMNS-836
	public void invokesCustomMethodIfItRedeclaresACRUDOne() {

		ObjectRepository repository = factory.getRepository(ObjectRepository.class, customImplementation);
		repository.findOne(1);

		verify(customImplementation, times(1)).findOne(1);
		verify(backingRepo, times(0)).findOne(1);
	}

	@Test // DATACMNS-836
	public void callsRxJava1MethodOnBaseImplementationWithExactArguments() {

		Serializable id = 1L;
		RxJava1ConvertingRepository repository = factory.getRepository(RxJava1ConvertingRepository.class);
		repository.exists(id);
		repository.exists((Long) id);

		verify(backingRepo, times(2)).exists(id);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void callsRxJava1MethodOnBaseImplementationWithTypeConversion() {

		Single<Long> ids = Single.just(1L);

		RxJava1ConvertingRepository repository = factory.getRepository(RxJava1ConvertingRepository.class);
		repository.exists(ids);

		verify(backingRepo, times(1)).exists(any(Mono.class));
	}

	@Test // DATACMNS-988
	public void callsRxJava2MethodOnBaseImplementationWithExactArguments() {

		Long id = 1L;
		RxJava2ConvertingRepository repository = factory.getRepository(RxJava2ConvertingRepository.class);
		repository.findOne(id);

		verify(backingRepo, times(1)).findOne(id);
	}

	@Test // DATACMNS-988
	public void callsRxJava2MethodOnBaseImplementationWithTypeConversion() {

		Serializable id = 1L;

		RxJava2ConvertingRepository repository = factory.getRepository(RxJava2ConvertingRepository.class);
		repository.delete(id);

		verify(backingRepo, times(1)).delete(id);
	}

	interface RxJava1ConvertingRepository extends Repository<Object, Long> {

		Single<Boolean> exists(Single<Long> id);

		Single<Boolean> exists(Serializable id);

		Single<Boolean> exists(Long id);
	}

	interface RxJava2ConvertingRepository extends Repository<Object, Long> {

		Maybe<Boolean> findOne(Serializable id);

		Single<Boolean> exists(Long id);

		Completable delete(Serializable id);
	}

	interface ObjectRepository
			extends Repository<Object, Serializable>, RepositoryFactorySupportUnitTests.ObjectRepositoryCustom {

	}

	interface ObjectRepositoryCustom {

		Object findOne(Serializable id);
	}
}
