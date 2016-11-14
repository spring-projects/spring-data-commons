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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.support.PageableExecutionUtils.TotalSupplier;

/**
 * Unit tests for {@link PageableExecutionUtils}.
 * 
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class PageableExecutionUtilsUnitTests {

	@Mock TotalSupplier totalSupplierMock;

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void firstPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 10),
				totalSupplierMock);

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);
		verifyZeroInteractions(totalSupplierMock);
	}

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void noPageableRequesDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), null, totalSupplierMock);

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);

		verifyZeroInteractions(totalSupplierMock);
	}

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void subsequentPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), new PageRequest(5, 10),
				totalSupplierMock);

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(53L);

		verifyZeroInteractions(totalSupplierMock);
	}

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void firstPageRequestHitsUpperBoundRequiresTotal() {

		doReturn(4L).when(totalSupplierMock).get();

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 3),
				totalSupplierMock);

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(4L);

		verify(totalSupplierMock).get();
	}

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void subsequentPageRequestHitsUpperBoundRequiresTotal() {

		doReturn(7L).when(totalSupplierMock).get();

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(1, 3),
				totalSupplierMock);

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(7L);

		verify(totalSupplierMock).get();
	}

	/**
	 * @see DATAMCNS-884
	 */
	@Test
	public void subsequentPageRequestWithoutResultRequiresRequireTotal() {

		doReturn(7L).when(totalSupplierMock).get();
		Page<Integer> page = PageableExecutionUtils.getPage(Collections.<Integer>emptyList(), new PageRequest(5, 10),
				totalSupplierMock);

		assertThat(page.getTotalElements()).isEqualTo(7L);

		verify(totalSupplierMock).get();
	}
}
