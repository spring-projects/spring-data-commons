/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.querydsl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;

import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslRepositoryInvokerAdapter}.
 * 
 * @author Oliver Gierke
 * @soundtrack Emilie Nicolas - Grown Up
 */
@RunWith(MockitoJUnitRunner.class)
public class QuerydslRepositoryInvokerAdapterUnitTests {

	@Mock RepositoryInvoker delegate;
	@Mock QueryDslPredicateExecutor<Object> executor;
	@Mock Predicate predicate;

	QuerydslRepositoryInvokerAdapter adapter;

	@Before
	public void setUp() {
		this.adapter = new QuerydslRepositoryInvokerAdapter(delegate, executor, predicate);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void forwardsFindAllToExecutorWithPredicate() {

		Sort sort = Sort.by("firstname");
		adapter.invokeFindAll(sort);

		verify(executor, times(1)).findAll(predicate, sort);
		verify(delegate, times(0)).invokeFindAll(sort);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void forwardsFindAllWithPageableToExecutorWithPredicate() {

		PageRequest pageable = PageRequest.of(0, 10);
		adapter.invokeFindAll(pageable);

		verify(executor, times(1)).findAll(predicate, pageable);
		verify(delegate, times(0)).invokeFindAll(pageable);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test

	public void forwardsMethodsToDelegate() {

		adapter.hasDeleteMethod();
		verify(delegate, times(1)).hasDeleteMethod();

		adapter.hasFindAllMethod();
		verify(delegate, times(1)).hasFindAllMethod();

		adapter.hasFindOneMethod();
		verify(delegate, times(1)).hasFindOneMethod();

		adapter.hasSaveMethod();
		verify(delegate, times(1)).hasSaveMethod();

		adapter.invokeDelete(any(Serializable.class));
		verify(delegate, times(1)).invokeDelete(any());

		adapter.invokeFindOne(any(Serializable.class));
		verify(delegate, times(1)).invokeFindOne(any());

		adapter.invokeQueryMethod(any(), any(), any(), any());

		verify(delegate, times(1)).invokeQueryMethod(any(), any(), any(), any());

		adapter.invokeSave(any());
		verify(delegate, times(1)).invokeSave(any());
	}
}
