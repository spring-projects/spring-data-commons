/*
 * Copyright 2016-2021 the original author or authors.
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

import static org.mockito.Mockito.*;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Mono;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

/**
 * Unit tests for {@link RepositoryFactorySupport} using reactive wrapper types.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveWrapperRepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock ReactiveSortingRepository<Object, Serializable> backingRepo;
	@Mock ObjectRepositoryCustom customImplementation;

	@BeforeEach
	void setUp() {
		factory = new DummyRepositoryFactory(backingRepo);
	}

	@Test // DATACMNS-836
	void invokesCustomMethodIfItRedeclaresACRUDOne() {

		var repository = factory.getRepository(ObjectRepository.class, customImplementation);
		repository.findById(1);

		verify(customImplementation, times(1)).findById(1);
		verify(backingRepo, times(0)).findById(1);
	}


	@Test // DATACMNS-988, DATACMNS-1154
	void callsRxJava2MethodOnBaseImplementationWithExactArguments() {

		Long id = 1L;
		when(backingRepo.findById(id)).thenReturn(Mono.just(true));

		var repository = factory.getRepository(RxJava3ConvertingRepository.class);
		repository.findById(id);

		verify(backingRepo, times(1)).findById(id);
	}

	@Test // DATACMNS-988, DATACMNS-1154
	void callsRxJava2MethodOnBaseImplementationWithTypeConversion() {

		Serializable id = 1L;
		when(backingRepo.deleteById(id)).thenReturn(Mono.empty());

		var repository = factory.getRepository(RxJava3ConvertingRepository.class);
		repository.deleteById(id);

		verify(backingRepo, times(1)).deleteById(id);
	}


	interface RxJava3ConvertingRepository extends Repository<Object, Long> {

		Maybe<Boolean> findById(Serializable id);

		Single<Boolean> existsById(Long id);

		Completable deleteById(Serializable id);
	}

	interface ObjectRepository
			extends Repository<Object, Object>, RepositoryFactorySupportUnitTests.ObjectRepositoryCustom {

	}

	interface ObjectRepositoryCustom {

		Object findById(Object id);
	}
}
