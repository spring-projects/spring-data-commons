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
package org.springframework.data.querydsl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.UnsupportedFragmentException;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RepositoryFactorySupport}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Ariel Carrera
 * @author Johannes Englmeier
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RepositoryFactorySupportUnitTests {

	DummyRepositoryFactory factory;

	@Mock CrudRepository<Object, Object> backingRepo;

	@BeforeEach
	void setUp() {
		factory = new DummyRepositoryFactory(backingRepo);
	}

	@Test
	void cachesRepositoryInformation() {

		var repository1 = factory.getRepository(ObjectAndQuerydslRepository.class, backingRepo);
		var repository2 = factory.getRepository(ObjectAndQuerydslRepository.class, backingRepo);
		repository1.findByFoo(null);
		repository2.deleteAll();

		for (int i = 0; i < 10; i++) {
			RepositoryFragments fragments = RepositoryFragments.just(backingRepo);
			RepositoryMetadata metadata = factory.getRepositoryMetadata(ObjectAndQuerydslRepository.class);
			factory.getRepositoryInformation(metadata, fragments);
		}

		Map<Object, RepositoryInformation> cache = (Map) ReflectionTestUtils.getField(factory,
				"repositoryInformationCache");

		assertThat(cache).hasSize(1);
	}

	@Test // GH-2341
	void dummyRepositoryShouldsupportQuerydsl() {
		factory.getRepository(WithQuerydsl.class, backingRepo);
	}

	@Test // GH-2341
	void dummyRepositoryNotSupportingReactiveQuerydslShouldRaiseException() {
		assertThatThrownBy(() -> factory.getRepository(WithReactiveQuerydsl.class, backingRepo))
				.isInstanceOf(UnsupportedFragmentException.class).hasMessage(
						"Repository org.springframework.data.querydsl.RepositoryFactorySupportUnitTests$WithReactiveQuerydsl implements org.springframework.data.repository.query.ReactiveQueryByExampleExecutor but DummyRepositoryFactory does not support Reactive Query by Example");
	}

	private void expect(Future<?> future, Object value) throws Exception {

		assertThat(future.isDone()).isFalse();

		while (!future.isDone()) {
			Thread.sleep(50);
		}

		assertThat(future.get()).isEqualTo(value);

		verify(factory.queryOne, times(1)).execute(any(Object[].class));
	}

	interface ObjectAndQuerydslRepository extends ObjectRepository, QuerydslPredicateExecutor<Object> {

	}

	interface ObjectRepository extends Repository<Object, Object>, ObjectRepositoryCustom {

		@org.jspecify.annotations.Nullable
		Object findByFoo(@org.jspecify.annotations.Nullable Object foo);

	}

	interface ObjectRepositoryCustom {

		@Nullable
		Object findById(Object id);

		void deleteAll();
	}

	interface WithQuerydsl extends Repository<Object, Long>, QuerydslPredicateExecutor<Object> {

	}

	interface WithReactiveQuerydsl extends Repository<Object, Long>, ReactiveQueryByExampleExecutor<Object> {

	}

}
