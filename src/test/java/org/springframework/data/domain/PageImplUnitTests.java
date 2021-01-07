/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.UnitTestUtils.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link PageImpl}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class PageImplUnitTests {

	@Test
	void assertEqualsForSimpleSetup() {

		PageImpl<String> page = new PageImpl<>(Collections.singletonList("Foo"));

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<>(Collections.singletonList("Foo")));
	}

	@Test
	void assertEqualsForComplexSetup() {

		Pageable pageable = PageRequest.of(0, 10);
		List<String> content = Collections.singletonList("Foo");

		PageImpl<String> page = new PageImpl<>(content, pageable, 100);

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<>(content, pageable, 100));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, pageable, 90));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, PageRequest.of(1, 10), 100));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, PageRequest.of(0, 15), 100));
	}

	@Test
	void preventsNullContentForSimpleSetup() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PageImpl<>(null));
	}

	@Test
	void preventsNullContentForAdvancedSetup() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PageImpl<>(null, null, 0));
	}

	@Test
	void returnsNextPageable() {

		Page<Object> page = new PageImpl<>(Collections.singletonList(new Object()), PageRequest.of(0, 1), 10);

		assertThat(page.isFirst()).isTrue();
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.previousPageable().isPaged()).isFalse();

		assertThat(page.isLast()).isFalse();
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextPageable()).isEqualTo(PageRequest.of(1, 1));
	}

	@Test
	void returnsPreviousPageable() {

		Page<Object> page = new PageImpl<>(Collections.singletonList(new Object()), PageRequest.of(1, 1), 2);

		assertThat(page.isFirst()).isFalse();
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.previousPageable()).isEqualTo(PageRequest.of(0, 1));

		assertThat(page.isLast()).isTrue();
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextPageable().isPaged()).isFalse();
	}

	@Test
	void createsPageForEmptyContentCorrectly() {

		List<String> list = Collections.emptyList();
		Page<String> page = new PageImpl<>(list);

		assertThat(page.getContent()).isEqualTo(list);
		assertThat(page.getNumber()).isEqualTo(0);
		assertThat(page.getNumberOfElements()).isEqualTo(0);
		assertThat(page.getSize()).isEqualTo(0);
		assertThat(page.getSort()).isEqualTo(Sort.unsorted());
		assertThat(page.getTotalElements()).isEqualTo(0L);
		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.isFirst()).isTrue();
		assertThat(page.isLast()).isTrue();
		assertThat(page.hasContent()).isFalse();
	}

	@Test // DATACMNS-323
	void returnsCorrectTotalPages() {

		Page<String> page = new PageImpl<>(Collections.singletonList("a"));

		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.hasPrevious()).isFalse();
	}

	@Test // DATACMNS-635
	void transformsPageCorrectly() {

		Page<Integer> transformed = new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 2), 10)
				.map(String::length);

		assertThat(transformed.getContent()).hasSize(2).contains(3, 3);
	}

	@Test // DATACMNS-713
	void adaptsTotalForLastPageOnIntermediateDeletion() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 5), 3).getTotalElements()).isEqualTo(2L);
	}

	@Test // DATACMNS-713
	void adaptsTotalForLastPageOnIntermediateInsertion() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 5), 1).getTotalElements()).isEqualTo(2L);
	}

	@Test // DATACMNS-713
	void adaptsTotalForLastPageOnIntermediateDeletionOnLastPate() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(1, 10), 13).getTotalElements())
				.isEqualTo(12L);
	}

	@Test // DATACMNS-713
	void adaptsTotalForLastPageOnIntermediateInsertionOnLastPate() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(1, 10), 11).getTotalElements())
				.isEqualTo(12L);
	}

	@Test // DATACMNS-713
	void doesNotAdapttotalIfPageIsEmpty() {

		assertThat(new PageImpl<>(Collections.<String> emptyList(), PageRequest.of(1, 10), 0).getTotalElements())
				.isEqualTo(0L);
	}

	@Test // DATACMNS-1476
	void returnsSelfPagablesIfThePageIsAlreadyTheFirstOrLastOne() {

		Pageable pageable = PageRequest.of(0, 2);
		Slice<String> page = new PageImpl<>(Arrays.asList("foo", "bar"), pageable, 2);

		assertThat(page.previousPageable()).isEqualTo(Pageable.unpaged());
		assertThat(page.previousOrFirstPageable()).isEqualTo(pageable);

		assertThat(page.nextPageable()).isEqualTo(Pageable.unpaged());
		assertThat(page.nextOrLastPageable()).isEqualTo(pageable);
	}

	@Test // DATACMNS-1613
	void usesContentLengthForSizeIfNoPageableGiven() {

		Page<Integer> page = new PageImpl<>(Arrays.asList(1, 2));

		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.hasNext()).isFalse();
	}

	@Test // DATACMNS-1750
	void toStringShouldNotInspectNullInstances() {

		Page<Integer> page = new PageImpl<>(Collections.singletonList(null));

		assertThat(page).hasToString("Page 1 of 1 containing UNKNOWN instances");
	}

}
