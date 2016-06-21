/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.mockito.Mockito.*;
import static org.springframework.data.repository.support.RepositoryInvocationTestUtils.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.support.RepositoryInvocationTestUtils.VerifyingMethodInterceptor;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Unit tests for {@link PagingAndSortingRepositoryInvoker}.
 * 
 * @author Oliver Gierke
 */
public class PaginginAndSortingRepositoryInvokerUnitTests {

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindAllWithPageableByDefault() throws Exception {

		Repository repository = mock(Repository.class);
		Method method = PagingAndSortingRepository.class.getMethod("findAll", Pageable.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(PageRequest.of(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.NONE);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesFindAllWithSortByDefault() throws Exception {

		Repository repository = mock(Repository.class);
		Method method = PagingAndSortingRepository.class.getMethod("findAll", Sort.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.by("foo"));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesRedeclaredFindAllWithPageable() throws Exception {

		RepositoryWithRedeclaredFindAllWithPageable repository = mock(RepositoryWithRedeclaredFindAllWithPageable.class);
		Method method = RepositoryWithRedeclaredFindAllWithPageable.class.getMethod("findAll", Pageable.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(PageRequest.of(0, 10));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Pageable.NONE);
	}

	/**
	 * @see DATACMNS-589
	 */
	@Test
	public void invokesRedeclaredFindAllWithSort() throws Exception {

		RepositoryWithRedeclaredFindAllWithSort repository = mock(RepositoryWithRedeclaredFindAllWithSort.class);
		Method method = RepositoryWithRedeclaredFindAllWithSort.class.getMethod("findAll", Sort.class);

		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.by("foo"));
		getInvokerFor(repository, expectInvocationOf(method)).invokeFindAll(Sort.unsorted());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static RepositoryInvoker getInvokerFor(Object repository) {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(repository.getClass().getInterfaces()[0]);
		GenericConversionService conversionService = new DefaultFormattingConversionService();

		return new PagingAndSortingRepositoryInvoker((PagingAndSortingRepository) repository, metadata, conversionService);
	}

	private static RepositoryInvoker getInvokerFor(Object repository, VerifyingMethodInterceptor interceptor) {
		return getInvokerFor(getVerifyingRepositoryProxy(repository, interceptor));
	}

	interface Repository extends PagingAndSortingRepository<Object, Long> {}

	interface RepositoryWithRedeclaredFindAllWithPageable extends PagingAndSortingRepository<Object, Long> {

		Page<Object> findAll(Pageable pageable);
	}

	interface RepositoryWithRedeclaredFindAllWithSort extends PagingAndSortingRepository<Object, Long> {

		Page<Object> findAll(Sort sort);
	}
}
