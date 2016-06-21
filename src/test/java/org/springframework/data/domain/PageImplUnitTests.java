/*
 * Copyright 2008-2015 the original author or authors.
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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.UnitTestUtils.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Unit test for {@link PageImpl}.
 * 
 * @author Oliver Gierke
 */
public class PageImplUnitTests {

	@Test
	public void assertEqualsForSimpleSetup() throws Exception {

		PageImpl<String> page = new PageImpl<>(Arrays.asList("Foo"));

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<>(Arrays.asList("Foo")));
	}

	@Test
	public void assertEqualsForComplexSetup() throws Exception {

		Pageable pageable = PageRequest.of(0, 10);
		List<String> content = Arrays.asList("Foo");

		PageImpl<String> page = new PageImpl<>(content, pageable, 100);

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<>(content, pageable, 100));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, pageable, 90));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, PageRequest.of(1, 10), 100));
		assertNotEqualsAndHashcode(page, new PageImpl<>(content, PageRequest.of(0, 15), 100));
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullContentForSimpleSetup() throws Exception {
		new PageImpl<>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullContentForAdvancedSetup() throws Exception {
		new PageImpl<>(null, null, 0);
	}

	@Test
	public void returnsNextPageable() {

		Page<Object> page = new PageImpl<>(Arrays.asList(new Object()), PageRequest.of(0, 1), 10);

		assertThat(page.isFirst()).isTrue();
		assertThat(page.hasPrevious()).isFalse();
		assertThat(page.previousPageable()).isEqualTo(Pageable.NONE);

		assertThat(page.isLast()).isFalse();
		assertThat(page.hasNext()).isTrue();
		assertThat(page.nextPageable()).isEqualTo((Pageable) PageRequest.of(1, 1));
	}

	@Test
	public void returnsPreviousPageable() {

		Page<Object> page = new PageImpl<>(Arrays.asList(new Object()), PageRequest.of(1, 1), 2);

		assertThat(page.isFirst()).isFalse();
		assertThat(page.hasPrevious()).isTrue();
		assertThat(page.previousPageable()).isEqualTo((Pageable) PageRequest.of(0, 1));

		assertThat(page.isLast()).isTrue();
		assertThat(page.hasNext()).isFalse();
		assertThat(page.nextPageable()).isEqualTo(Pageable.NONE);
	}

	@Test
	public void createsPageForEmptyContentCorrectly() {

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

	/**
	 * @see DATACMNS-323
	 */
	@Test
	public void returnsCorrectTotalPages() {

		Page<String> page = new PageImpl<>(Arrays.asList("a"));

		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
		assertThat(page.hasPrevious()).isFalse();
	}

	/**
	 * @see DATACMNS-635
	 */
	@Test
	public void transformsPageCorrectly() {

		Page<Integer> transformed = new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 2), 10)
				.map(source -> source.length());

		assertThat(transformed.getContent()).hasSize(2);
		assertThat(transformed.getContent()).contains(3, 3);
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateDeletion() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 5), 3).getTotalElements()).isEqualTo(2L);
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateInsertion() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(0, 5), 1).getTotalElements()).isEqualTo(2L);
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateDeletionOnLastPate() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(1, 10), 13).getTotalElements())
				.isEqualTo(12L);
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateInsertionOnLastPate() {
		assertThat(new PageImpl<>(Arrays.asList("foo", "bar"), PageRequest.of(1, 10), 11).getTotalElements())
				.isEqualTo(12L);
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void doesNotAdapttotalIfPageIsEmpty() {

		assertThat(new PageImpl<>(Collections.<String>emptyList(), PageRequest.of(1, 10), 0).getTotalElements())
				.isEqualTo(0L);
	}
}
