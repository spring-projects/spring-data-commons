/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.InOrder;

/**
 * Unit tests for {@link QueryPostProcessor}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.mockito.Mockito
 * @see org.springframework.data.repository.query.QueryPostProcessor
 * @since 1.0.0
 */
public class QueryPostProcessorUnitTests {

	@Test
	@SuppressWarnings("unchecked")
	public void processAfterReturnsCompositeQueryPostProcessorAndPostProcessesInOrder() {

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		String query = "SELECT * FROM /Test";

		QueryPostProcessor<?, String> mockQueryPostProcessorOne = mock(QueryPostProcessor.class);
		QueryPostProcessor<?, String> mockQueryPostProcessorTwo = mock(QueryPostProcessor.class);

		when(mockQueryPostProcessorOne.processAfter(any())).thenCallRealMethod();
		when(mockQueryPostProcessorOne.postProcess(any(QueryMethod.class), anyString(), any())).thenReturn(query);
		when(mockQueryPostProcessorTwo.postProcess(any(QueryMethod.class), anyString(), any())).thenReturn(query);

		QueryPostProcessor<?, String> composite = mockQueryPostProcessorOne.processAfter(mockQueryPostProcessorTwo);

		assertThat(composite).isNotNull();
		assertThat(composite).isNotSameAs(mockQueryPostProcessorOne);
		assertThat(composite).isNotSameAs(mockQueryPostProcessorTwo);
		assertThat(composite.postProcess(mockQueryMethod, query)).isEqualTo(query);

		InOrder inOrder = inOrder(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);

		inOrder.verify(mockQueryPostProcessorTwo, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), any());

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), any());
	}

	@Test
	public void processAfterReturnsThis() {

		QueryPostProcessor<?, ?> mockQueryPostProcessor = mock(QueryPostProcessor.class);

		when(mockQueryPostProcessor.processAfter(any())).thenCallRealMethod();

		assertThat(mockQueryPostProcessor.processAfter(null)).isSameAs(mockQueryPostProcessor);
	}
	@Test
	@SuppressWarnings("unchecked")
	public void processBeforeReturnsCompositeQueryPostProcessorAndPostProcessesInOrder() {

		QueryMethod mockQueryMethod = mock(QueryMethod.class);

		String query = "SELECT * FROM /Test";

		QueryPostProcessor<?, String> mockQueryPostProcessorOne = mock(QueryPostProcessor.class);
		QueryPostProcessor<?, String> mockQueryPostProcessorTwo = mock(QueryPostProcessor.class);

		when(mockQueryPostProcessorOne.processBefore(any())).thenCallRealMethod();
		when(mockQueryPostProcessorOne.postProcess(any(QueryMethod.class), anyString(), any())).thenReturn(query);
		when(mockQueryPostProcessorTwo.postProcess(any(QueryMethod.class), anyString(), any())).thenReturn(query);

		QueryPostProcessor<?, String> composite = mockQueryPostProcessorOne.processBefore(mockQueryPostProcessorTwo);

		assertThat(composite).isNotNull();
		assertThat(composite).isNotSameAs(mockQueryPostProcessorOne);
		assertThat(composite).isNotSameAs(mockQueryPostProcessorTwo);
		assertThat(composite.postProcess(mockQueryMethod, query)).isEqualTo(query);

		InOrder inOrder = inOrder(mockQueryPostProcessorOne, mockQueryPostProcessorTwo);

		inOrder.verify(mockQueryPostProcessorOne, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), any());

		inOrder.verify(mockQueryPostProcessorTwo, times(1))
			.postProcess(eq(mockQueryMethod), eq(query), any());
	}

	@Test
	public void processBeforeReturnsThis() {

		QueryPostProcessor<?, ?> mockQueryPostProcessor = mock(QueryPostProcessor.class);

		when(mockQueryPostProcessor.processBefore(any())).thenCallRealMethod();

		assertThat(mockQueryPostProcessor.processBefore(null)).isSameAs(mockQueryPostProcessor);
	}
}
