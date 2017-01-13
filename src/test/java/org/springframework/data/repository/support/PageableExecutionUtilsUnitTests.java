/*
 * Copyright 2016-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
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

	@Test // DATAMCNS-884
	public void firstPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), new PageRequest(0, 10),
				totalSupplierMock);

		assertThat(page, hasItems(1, 2, 3));
		assertThat(page.getTotalElements(), is(3L));
		verifyZeroInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884
	public void noPageableRequesDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), null, totalSupplierMock);

		assertThat(page, hasItems(1, 2, 3));
		assertThat(page.getTotalElements(), is(3L));

		verifyZeroInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884
	public void subsequentPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), new PageRequest(5, 10),
				totalSupplierMock);

		assertThat(page, hasItems(1, 2, 3));
		assertThat(page.getTotalElements(), is(53L));

		verifyZeroInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884
	public void firstPageRequestHitsUpperBoundRequiresTotal() {

		doReturn(4L).when(totalSupplierMock).get();

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), new PageRequest(0, 3),
				totalSupplierMock);

		assertThat(page, hasItems(1, 2, 3));
		assertThat(page.getTotalElements(), is(4L));

		verify(totalSupplierMock).get();
	}

	@Test // DATAMCNS-884
	public void subsequentPageRequestHitsUpperBoundRequiresTotal() {

		doReturn(7L).when(totalSupplierMock).get();

		Page<Integer> page = PageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), new PageRequest(1, 3),
				totalSupplierMock);

		assertThat(page, hasItems(1, 2, 3));
		assertThat(page.getTotalElements(), is(7L));

		verify(totalSupplierMock).get();
	}

	@Test // DATAMCNS-884
	public void subsequentPageRequestWithoutResultRequiresRequireTotal() {

		doReturn(7L).when(totalSupplierMock).get();
		Page<Integer> page = PageableExecutionUtils.getPage(Collections.<Integer>emptyList(), new PageRequest(5, 10),
				totalSupplierMock);

		assertThat(page.getTotalElements(), is(7L));

		verify(totalSupplierMock).get();
	}
}
