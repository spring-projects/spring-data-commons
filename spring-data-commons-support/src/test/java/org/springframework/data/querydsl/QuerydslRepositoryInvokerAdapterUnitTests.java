/*
 * Copyright 2015-2025 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.RepositoryInvoker;

import com.querydsl.core.types.Predicate;

/**
 * Unit tests for {@link QuerydslRepositoryInvokerAdapter}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Emilie Nicolas - Grown Up
 */
@ExtendWith(MockitoExtension.class)
class QuerydslRepositoryInvokerAdapterUnitTests {

	@Mock RepositoryInvoker delegate;
	@Mock QuerydslPredicateExecutor<Object> executor;
	@Mock Predicate predicate;

	QuerydslRepositoryInvokerAdapter adapter;

	@BeforeEach
	void setUp() {
		this.adapter = new QuerydslRepositoryInvokerAdapter(delegate, executor, predicate);
	}

	@Test // GH-1501
	void rejectsNullPredicate() {

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new QuerydslRepositoryInvokerAdapter(delegate, executor, null));
	}

	@Test // DATACMNS-669
	void forwardsFindAllToExecutorWithPredicate() {

		var sort = Sort.by("firstname");
		adapter.invokeFindAll(sort);

		verify(executor, times(1)).findAll(predicate, sort);
		verify(delegate, times(0)).invokeFindAll(sort);
	}

	@Test // DATACMNS-669
	void forwardsFindAllWithPageableToExecutorWithPredicate() {

		var pageable = PageRequest.of(0, 10);
		adapter.invokeFindAll(pageable);

		verify(executor, times(1)).findAll(predicate, pageable);
		verify(delegate, times(0)).invokeFindAll(pageable);
	}

	@Test // DATACMNS-669
	@SuppressWarnings("unchecked")
	void forwardsMethodsToDelegate() {

		adapter.hasDeleteMethod();
		verify(delegate, times(1)).hasDeleteMethod();

		adapter.hasFindAllMethod();
		verify(delegate, times(1)).hasFindAllMethod();

		adapter.hasFindOneMethod();
		verify(delegate, times(1)).hasFindOneMethod();

		adapter.hasSaveMethod();
		verify(delegate, times(1)).hasSaveMethod();

		adapter.invokeDeleteById(any(Serializable.class));
		verify(delegate, times(1)).invokeDeleteById(any());

		adapter.invokeFindById(any(Serializable.class));
		verify(delegate, times(1)).invokeFindById(any());

		adapter.invokeQueryMethod(any(), any(), any(), any());

		verify(delegate, times(1)).invokeQueryMethod(any(), any(), any(), any());

		adapter.invokeSave(any());
		verify(delegate, times(1)).invokeSave(any());
	}
}
