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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

	@Spy ReactiveSortingRepository<Integer, Integer> repository;

	@DisplayName("Entity fetching tests")
	@Nested
	class EntityFetchingTests {

		@DisplayName("Return a single entity for a limit of one")
		@Test
		void shouldReturnOnlyOneEntityForLimitOfOne() {
			when(repository.findAll(Sort.unsorted())).thenReturn(Flux.range(1, 20));

			final Flux<Integer> results = repository.findAll(Sort.unsorted(), Limit.of(1));

			StepVerifier.create(results)
					.expectNext(1)
					.expectComplete()
					.verify();

			verify(repository).findAll(Sort.unsorted());
		}

		@DisplayName("Return ten entities for a limit of ten")
		@Test
		void shouldReturnTenEntitiesForLimitOfTen() {
			when(repository.findAll(Sort.unsorted())).thenReturn(Flux.range(1, 20));

			final Flux<Integer> results = repository.findAll(Sort.unsorted(), Limit.of(10));

			StepVerifier.create(results)
					.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
					.expectComplete()
					.verify();
			
			verify(repository).findAll(Sort.unsorted());
		}

		@DisplayName("Return all entities with unlimited")
		@Test
		void shouldReturnAllEntitiesForUnlimited() {
			when(repository.findAll(Sort.unsorted())).thenReturn(Flux.range(1, 20));

			final Flux<Integer> results = repository.findAll(Sort.unsorted(), Limit.unlimited());

			StepVerifier.create(results)
					.expectNextSequence(IntStream.rangeClosed(1, 20).boxed().toList())
					.expectComplete()
					.verify();

			verify(repository).findAll(Sort.unsorted());
		}

		@DisplayName("Return all entities with maximum integer limit")
		@Test
		void shouldReturnAllEntitiesForLimitOfMaxValue() {
			when(repository.findAll(Sort.unsorted())).thenReturn(Flux.range(1, 20));

			final Flux<Integer> results = repository.findAll(Sort.unsorted(), Limit.of(Integer.MAX_VALUE));

			StepVerifier.create(results)
					.expectNextSequence(IntStream.rangeClosed(1, 20).boxed().toList())
					.expectComplete()
					.verify();

			verify(repository).findAll(Sort.unsorted());
		}

		@DisplayName("Exception tests for invalid limits")
		@Nested
		class ExceptionTests {

			@DisplayName("Throw IllegalArgumentException for null limit")
			@Test
			void shouldThrowIllegalArgumentExceptionWhenNullLimitIsGiven() {
				assertThrows(IllegalArgumentException.class, () -> repository.findAll(Sort.unsorted(), null));
			}

			@DisplayName("Throw IllegalArgumentException for invalid limit values")
			@ParameterizedTest
			@ValueSource(ints = { -1, Integer.MIN_VALUE, 0 })
			void shouldThrowIllegalArgumentExceptionForInvalidLimits(int limitValue) {
				assertThrows(IllegalArgumentException.class, () -> repository.findAll(Sort.unsorted(), Limit.of(limitValue)));
			}
		}
	}
}
