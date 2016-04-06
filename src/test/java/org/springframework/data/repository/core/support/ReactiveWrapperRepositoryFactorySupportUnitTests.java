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

import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.reactive.ReactivePagingAndSortingRepository;
import org.springframework.data.repository.util.QueryExecutionConverters;

import reactor.core.publisher.Mono;
import rx.Single;

/**
 * Unit tests for {@link RepositoryFactorySupport} using reactive wrapper types.
 * 
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ReactiveWrapperRepositoryFactorySupportUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	DummyRepositoryFactory factory;

	@Mock ReactivePagingAndSortingRepository<Object, Serializable> backingRepo;
	@Mock ObjectRepositoryCustom customImplementation;

	@Before
	public void setUp() {

		DefaultConversionService defaultConversionService = new DefaultConversionService();
		QueryExecutionConverters.registerConvertersIn(defaultConversionService);

		factory = new DummyRepositoryFactory(backingRepo);
		factory.setConversionService(defaultConversionService);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void invokesCustomMethodIfItRedeclaresACRUDOne() {

		ObjectRepository repository = factory.getRepository(ObjectRepository.class, customImplementation);
		repository.findOne(1);

		verify(customImplementation, times(1)).findOne(1);
		verify(backingRepo, times(0)).findOne(1);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void callsMethodOnBaseImplementationWithExactArguments() {

		Serializable id = 1L;
		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);
		repository.exists(id);

		verify(backingRepo, times(1)).exists(id);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void doesNotCallMethodOnBaseEvenIfDeclaredTypeCouldBeConverted() {

		Long id = 1L;
		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);
		repository.exists(id);

		verifyZeroInteractions(backingRepo);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void callsMethodOnBaseImplementationWithTypeConversion() {

		Single<Long> ids = Single.just(1L);

		ConvertingRepository repository = factory.getRepository(ConvertingRepository.class);
		repository.exists(ids);

		verify(backingRepo, times(1)).exists(any(Mono.class));
	}

	interface ConvertingRepository extends Repository<Object, Long> {

		Single<Boolean> exists(Single<Long> id);

		Single<Boolean> exists(Serializable id);

		Single<Boolean> exists(Long id);
	}

	interface ObjectRepository
			extends Repository<Object, Serializable>, RepositoryFactorySupportUnitTests.ObjectRepositoryCustom {

	}

	interface ObjectRepositoryCustom {

		Object findOne(Serializable id);
	}
}
