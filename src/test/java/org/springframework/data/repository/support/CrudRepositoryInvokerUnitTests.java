/*
 * Copyright 2014-2021 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Unit tests for {@link CrudRepositoryInvoker}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class CrudRepositoryInvokerUnitTests {

	@Mock PersonRepository personRepository;
	@Mock OrderRepository orderRepository;

	@Test // DATACMNS-589, DATAREST-216
	void invokesRedeclaredSave() {

		when(orderRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

		getInvokerFor(orderRepository, expectInvocationOnType(OrderRepository.class)).invokeSave(new Order());
	}

	@Test // DATACMNS-589, DATAREST-216
	void invokesRedeclaredFindOne() {
		getInvokerFor(orderRepository, expectInvocationOnType(OrderRepository.class)).invokeFindById(1L);
	}

	@Test // DATACMNS-589
	void invokesRedeclaredDelete() throws Exception {
		getInvokerFor(orderRepository, expectInvocationOnType(OrderRepository.class)).invokeDeleteById(1L);
	}

	@Test // DATACMNS-589
	void invokesSaveOnCrudRepository() throws Exception {

		Method method = CrudRepository.class.getMethod("save", Object.class);
		getInvokerFor(personRepository, expectInvocationOf(method)).invokeSave(new Person());
	}

	@Test // DATACMNS-589
	void invokesFindOneOnCrudRepository() throws Exception {

		Method method = CrudRepository.class.getMethod("findById", Object.class);
		getInvokerFor(personRepository, expectInvocationOf(method)).invokeFindById(1L);
	}

	@Test // DATACMNS-589, DATAREST-216
	void invokesDeleteOnCrudRepository() throws Exception {

		Method method = CrudRepository.class.getMethod("deleteById", Object.class);
		getInvokerFor(personRepository, expectInvocationOf(method)).invokeDeleteById(1L);
	}

	@Test // DATACMNS-589
	void invokesFindAllOnCrudRepository() throws Exception {

		Method method = CrudRepository.class.getMethod("findAll");

		getInvokerFor(orderRepository, expectInvocationOf(method)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(orderRepository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
	}

	@Test // DATACMNS-589
	void invokesCustomFindAllTakingASort() throws Exception {

		CrudWithFindAllWithSort repository = mock(CrudWithFindAllWithSort.class);

		Method findAllWithSort = CrudWithFindAllWithSort.class.getMethod("findAll", Sort.class);

		getInvokerFor(repository, expectInvocationOf(findAllWithSort)).invokeFindAll(Sort.unsorted());
		getInvokerFor(repository, expectInvocationOf(findAllWithSort)).invokeFindAll(PageRequest.of(0, 10));
		getInvokerFor(repository, expectInvocationOf(findAllWithSort)).invokeFindAll(Pageable.unpaged());
	}

	@Test // DATACMNS-589
	void invokesCustomFindAllTakingAPageable() throws Exception {

		CrudWithFindAllWithPageable repository = mock(CrudWithFindAllWithPageable.class);

		Method findAllWithPageable = CrudWithFindAllWithPageable.class.getMethod("findAll", Pageable.class);

		getInvokerFor(repository, expectInvocationOf(findAllWithPageable)).invokeFindAll(Pageable.unpaged());
		getInvokerFor(repository, expectInvocationOf(findAllWithPageable)).invokeFindAll(PageRequest.of(0, 10));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static RepositoryInvoker getInvokerFor(Object repository, VerifyingMethodInterceptor interceptor) {

		Object proxy = getVerifyingRepositoryProxy(repository, interceptor);

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repository.getClass().getInterfaces()[0]);
		GenericConversionService conversionService = new DefaultFormattingConversionService();

		return new CrudRepositoryInvoker((CrudRepository) proxy, metadata, conversionService);
	}

	static class Order {}

	interface OrderRepository extends CrudRepository<Order, Long> {

		@Override
		<S extends Order> S save(S entity);

		@Override
		Optional<Order> findById(Long id);

		@Override
		void deleteById(Long id);
	}

	static class Person {}

	interface PersonRepository extends PagingAndSortingRepository<Person, Long> {

		Page<Person> findByFirstName(@Param("firstName") String firstName, Pageable pageable);

		Page<Person> findByCreatedUsingISO8601Date(@Param("date") @DateTimeFormat(iso = ISO.DATE_TIME) Date date,
				Pageable pageable);

		List<Person> findByIdIn(@Param("ids") Collection<Long> ids);
	}

	interface CrudWithFindAllWithSort extends CrudRepository<Order, Long> {

		List<Order> findAll(Sort sort);
	}

	interface CrudWithFindAllWithPageable extends CrudRepository<Order, Long> {

		List<Order> findAll(Pageable sort);
	}

	interface CrudWithRedeclaredDelete extends CrudRepository<Order, Long> {

		void deleteById(Long id);
	}
}
