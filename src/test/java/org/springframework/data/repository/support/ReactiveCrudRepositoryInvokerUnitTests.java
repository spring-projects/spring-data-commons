/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.data.repository.support;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.format.support.DefaultFormattingConversionService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link ReactiveCrudRepositoryInvoker}.
 *
 * @author Gichan Im
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCrudRepositoryInvokerUnitTests {

	@Mock ReactivePersonRepository repository;
	@Mock ReactiveOrderRepository orderRepository;

	@Test // GH-2090
	void invokesRedeclaredSave() {

		when(orderRepository.save(any())).thenReturn(Mono.just(new Order()));

		getInvokerFor(orderRepository, expectInvocationOnType(ReactiveOrderRepository.class)).invokeSave(new Order());
	}

	@Test // GH-2090
	void invokesRedeclaredFindById() {

		when(orderRepository.findById(any(Long.class))).thenReturn(Mono.empty());

		getInvokerFor(orderRepository, expectInvocationOnType(ReactiveOrderRepository.class)).invokeFindById(1L);
	}

	@Test // GH-2090
	void invokesRedeclaredDelete() {

		when(orderRepository.deleteById(any(Long.class))).thenReturn(Mono.empty());

		getInvokerFor(orderRepository, expectInvocationOnType(ReactiveOrderRepository.class)).invokeDeleteById(1L);
	}

	@Test // GH-2090
	void invokesSaveOnReactiveCrudRepository() throws Exception {

		when(repository.save(any())).thenReturn(Mono.just(new Person()));

		var method = ReactiveCrudRepository.class.getMethod("save", Object.class);
		getInvokerFor(repository, expectInvocationOf(method)).invokeSave(new Person());
	}

	@Test // GH-2090
	void invokesFindByIdOnReactiveCrudRepository() throws Exception {

		when(repository.findById(any(Long.class))).thenReturn(Mono.empty());

		var method = ReactiveCrudRepository.class.getMethod("findById", Object.class);
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindById(1L);
	}

	@Test // GH-2090
	void invokesDeleteByIdOnReactiveCrudRepository() throws Exception {

		when(repository.deleteById(any(Long.class))).thenReturn(Mono.empty());

		var method = ReactiveCrudRepository.class.getMethod("deleteById", Object.class);
		getInvokerFor(repository, expectInvocationOf(method)).invokeDeleteById(1L);
	}

	@Test // GH-2090
	void invokesFindAllOnReactiveCrudRepository() throws Exception {

		when(repository.findAll()).thenReturn(Flux.empty());

		var method = ReactiveCrudRepository.class.getMethod("findAll");
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static RepositoryInvoker getInvokerFor(Object repository, VerifyingMethodInterceptor interceptor) {

		var proxy = getVerifyingRepositoryProxy(repository, interceptor);

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repository.getClass().getInterfaces()[0]);
		GenericConversionService conversionService = new DefaultFormattingConversionService();

		return new ReactiveCrudRepositoryInvoker((ReactiveCrudRepository) proxy, metadata, conversionService);
	}

	static class Person {}

	static class Order {}

	interface ReactivePersonRepository extends ReactiveCrudRepository<Person, Long> {}

	interface ReactiveOrderRepository extends ReactiveCrudRepository<Order, Long> {

		@Override
		<S extends Order> Mono<S> save(S entity);

		@Override
		Mono<Order> findById(Long id);

		@Override
		Mono<Void> deleteById(Long id);
	}
}
