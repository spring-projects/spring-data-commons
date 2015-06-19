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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.UnitTestUtils.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.converter.Converter;

/**
 * Unit test for {@link PageImpl}.
 * 
 * @author Oliver Gierke
 */
public class PageImplUnitTests {

	@Test
	public void assertEqualsForSimpleSetup() throws Exception {

		PageImpl<String> page = new PageImpl<String>(Arrays.asList("Foo"));

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<String>(Arrays.asList("Foo")));
	}

	@Test
	public void assertEqualsForComplexSetup() throws Exception {

		Pageable pageable = new PageRequest(0, 10);
		List<String> content = Arrays.asList("Foo");

		PageImpl<String> page = new PageImpl<String>(content, pageable, 100);

		assertEqualsAndHashcode(page, page);
		assertEqualsAndHashcode(page, new PageImpl<String>(content, pageable, 100));
		assertNotEqualsAndHashcode(page, new PageImpl<String>(content, pageable, 90));
		assertNotEqualsAndHashcode(page, new PageImpl<String>(content, new PageRequest(1, 10), 100));
		assertNotEqualsAndHashcode(page, new PageImpl<String>(content, new PageRequest(0, 15), 100));
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullContentForSimpleSetup() throws Exception {
		new PageImpl<Object>(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullContentForAdvancedSetup() throws Exception {
		new PageImpl<Object>(null, null, 0);
	}

	@Test
	public void returnsNextPageable() {

		Page<Object> page = new PageImpl<Object>(Arrays.asList(new Object()), new PageRequest(0, 1), 10);

		assertThat(page.isFirst(), is(true));
		assertThat(page.hasPrevious(), is(false));
		assertThat(page.previousPageable(), is(nullValue()));

		assertThat(page.isLast(), is(false));
		assertThat(page.hasNext(), is(true));
		assertThat(page.nextPageable(), is((Pageable) new PageRequest(1, 1)));
	}

	@Test
	public void returnsPreviousPageable() {

		Page<Object> page = new PageImpl<Object>(Arrays.asList(new Object()), new PageRequest(1, 1), 2);

		assertThat(page.isFirst(), is(false));
		assertThat(page.hasPrevious(), is(true));
		assertThat(page.previousPageable(), is((Pageable) new PageRequest(0, 1)));

		assertThat(page.isLast(), is(true));
		assertThat(page.hasNext(), is(false));
		assertThat(page.nextPageable(), is(nullValue()));
	}

	@Test
	public void createsPageForEmptyContentCorrectly() {

		List<String> list = Collections.emptyList();
		Page<String> page = new PageImpl<String>(list);

		assertThat(page.getContent(), is(list));
		assertThat(page.getNumber(), is(0));
		assertThat(page.getNumberOfElements(), is(0));
		assertThat(page.getSize(), is(0));
		assertThat(page.getSort(), is((Sort) null));
		assertThat(page.getTotalElements(), is(0L));
		assertThat(page.getTotalPages(), is(1));
		assertThat(page.hasNext(), is(false));
		assertThat(page.hasPrevious(), is(false));
		assertThat(page.isFirst(), is(true));
		assertThat(page.isLast(), is(true));
		assertThat(page.hasContent(), is(false));
	}

	/**
	 * @see DATACMNS-323
	 */
	@Test
	public void returnsCorrectTotalPages() {

		Page<String> page = new PageImpl<String>(Arrays.asList("a"));

		assertThat(page.getTotalPages(), is(1));
		assertThat(page.hasNext(), is(false));
		assertThat(page.hasPrevious(), is(false));
	}

	/**
	 * @see DATACMNS-635
	 */
	@Test
	public void transformsPageCorrectly() {

		Page<Integer> transformed = new PageImpl<String>(Arrays.asList("foo", "bar"), new PageRequest(0, 2), 10)
				.map(new Converter<String, Integer>() {
					@Override
					public Integer convert(String source) {
						return source.length();
					}
				});

		assertThat(transformed.getContent(), hasSize(2));
		assertThat(transformed.getContent(), contains(3, 3));
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateDeletion() {
		assertThat(new PageImpl<String>(Arrays.asList("foo", "bar"), new PageRequest(0, 5), 3).getTotalElements(), is(2L));
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateInsertion() {
		assertThat(new PageImpl<String>(Arrays.asList("foo", "bar"), new PageRequest(0, 5), 1).getTotalElements(), is(2L));
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateDeletionOnLastPate() {
		assertThat(new PageImpl<String>(Arrays.asList("foo", "bar"), new PageRequest(1, 10), 13).getTotalElements(),
				is(12L));
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void adaptsTotalForLastPageOnIntermediateInsertionOnLastPate() {
		assertThat(new PageImpl<String>(Arrays.asList("foo", "bar"), new PageRequest(1, 10), 11).getTotalElements(),
				is(12L));
	}

	/**
	 * @see DATACMNS-713
	 */
	@Test
	public void doesNotAdapttotalIfPageIsEmpty() {

		assertThat(new PageImpl<String>(Collections.<String> emptyList(), new PageRequest(1, 10), 0).getTotalElements(),
				is(0L));
	}
}
