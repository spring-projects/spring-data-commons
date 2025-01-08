/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.data.support;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link ReactivePageableExecutionUtils}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ReactivePageableExecutionUtilsUnitTests {

	@Test // DATAMCNS-884, GH-3209
	void firstPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Mono<Long> totalSupplierMock = mock(Mono.class);
		var page = ReactivePageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 10), totalSupplierMock)
				.block();

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);
		verifyNoInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884, GH-3209
	void noPageableRequestDoesNotRequireTotal() {

		Mono<Long> totalSupplierMock = mock(Mono.class);
		var page = ReactivePageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), Pageable.unpaged(), totalSupplierMock)
				.block();

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(3L);

		verifyNoInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884, GH-3209
	void subsequentPageRequestIsLessThanOneFullPageDoesNotRequireTotal() {

		Mono<Long> totalSupplierMock = mock(Mono.class);
		var page = ReactivePageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(5, 10), totalSupplierMock)
				.block();

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(53L);

		verifyNoInteractions(totalSupplierMock);
	}

	@Test // DATAMCNS-884, GH-3209
	void firstPageRequestHitsUpperBoundRequiresTotal() {

		Mono<Long> totalSupplierMock = Mono.just(4L);

		var page = ReactivePageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(0, 3), totalSupplierMock)
				.block();

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAMCNS-884, GH-3209
	void subsequentPageRequestHitsUpperBoundRequiresTotal() {

		Mono<Long> totalSupplierMock = Mono.just(7L);

		var page = ReactivePageableExecutionUtils.getPage(Arrays.asList(1, 2, 3), PageRequest.of(1, 3), totalSupplierMock)
				.block();

		assertThat(page).contains(1, 2, 3);
		assertThat(page.getTotalElements()).isEqualTo(7L);
	}

	@Test // DATAMCNS-884, GH-3209
	void subsequentPageRequestWithoutResultRequiresRequireTotal() {

		Mono<Long> totalSupplierMock = Mono.just(7L);
		var page = ReactivePageableExecutionUtils.getPage(emptyList(), PageRequest.of(5, 10), totalSupplierMock).block();

		assertThat(page.getTotalElements()).isEqualTo(7L);
	}
}
