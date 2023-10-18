/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.data.repository.reactive;

import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link ReactiveSortingRepository}.
 *
 * @author YongHwan Kwon
 */
@ExtendWith(MockitoExtension.class)
class ReactiveSortingRepositoryTest {

	private static final Limit LIMIT_TWO = Limit.of(2);
	private static final Limit LIMIT_NEGATIVE_ONE = Limit.of(-1);
	private static final Limit LIMIT_ZERO = Limit.of(0);
	private final Sort defaultSort = Sort.unsorted();
	@Spy ReactiveSortingRepository<DummyEntity, String> repository;

	record DummyEntity(Integer id) {

	}

	@DisplayName("Entity fetching tests")
	@Nested
	class EntityFetchingTests {

		@DisplayName("Return only the limited number of entities when a limit is given")
		@Test
		void shouldReturnLimitedEntitiesWhenLimitIsGiven() {
			when(repository.findAll(defaultSort)).thenReturn(Flux.fromIterable(List.of(new DummyEntity(1), new DummyEntity(2), new DummyEntity(3))));

			final Flux<DummyEntity> results = repository.findAll(defaultSort, LIMIT_TWO);

			StepVerifier.create(results).expectNextMatches(entity -> 1 == entity.id())
					.expectNextMatches(entity -> 2 == entity.id()).expectComplete().verify();

			verify(repository).findAll(defaultSort);
		}

		@DisplayName("Return all entities when no limit is set")
		@Test
		void shouldReturnAllEntitiesWhenNoLimitIsSet() {
			when(repository.findAll(defaultSort)).thenReturn(Flux.fromIterable(List.of(new DummyEntity(1), new DummyEntity(2), new DummyEntity(3))));

			final Flux<DummyEntity> results = repository.findAll(defaultSort, Limit.unlimited());

			StepVerifier.create(results).expectNextMatches(entity -> 1 == entity.id())
					.expectNextMatches(entity -> 2 == entity.id()).expectNextMatches(entity -> 3 == entity.id()).expectComplete()
					.verify();

			verify(repository).findAll(defaultSort);
		}

		@DisplayName("Exception tests for invalid limits")
		@Nested
		class ExceptionTests {

			@DisplayName("Throw IllegalArgumentException when null limit is given")
			@Test
			void shouldThrowExceptionWhenNullLimitIsGiven() {
				Flux<DummyEntity> resultFlux;
				try {
					resultFlux = repository.findAll(defaultSort, null);
				} catch (Exception e) {
					resultFlux = Flux.error(e);
				}

				StepVerifier.create(resultFlux).expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
						&& "Limit must not be null".equals(throwable.getMessage())).verify();
			}

			@DisplayName("Throw IllegalArgumentException when a negative limit is given")
			@Test
			void shouldThrowExceptionWhenNegativeLimitIsGiven() {
				Flux<DummyEntity> resultFlux;
				try {
					resultFlux = repository.findAll(defaultSort, LIMIT_NEGATIVE_ONE);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					resultFlux = Flux.error(e);
				}

				StepVerifier.create(resultFlux).expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
						&& "Limit value cannot be negative".equals(throwable.getMessage())).verify();
			}

			@DisplayName("Throw IllegalArgumentException when a zero limit is given")
			@Test
			void shouldThrowExceptionWhenZeroLimitIsGiven() {
				Flux<DummyEntity> resultFlux;
				try {
					resultFlux = repository.findAll(defaultSort, LIMIT_ZERO);
				} catch (Exception e) {
					resultFlux = Flux.error(e);
				}

				StepVerifier.create(resultFlux).expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
						&& "Limit value cannot be zero".equals(throwable.getMessage())).verify();
			}
		}
	}
}
