/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.NullHandling.*;

import org.junit.Test;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

/**
 * Unit test for {@link Sort}.
 * 
 * @author Oliver Gierke
 * @author Kevin Raymond
 * @author Thomas Darimont
 */
public class SortUnitTests {

	/**
	 * Asserts that the class applies the default sort order if no order or {@code null} was provided.
	 * 
	 * @throws Exception
	 */
	@Test
	public void appliesDefaultForOrder() throws Exception {

		assertEquals(Sort.DEFAULT_DIRECTION, new Sort("foo").iterator().next().getDirection());
		assertEquals(Sort.DEFAULT_DIRECTION, new Sort((Direction) null, "foo").iterator().next().getDirection());
	}

	/**
	 * Asserts that the class rejects {@code null} as properties array.
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullProperties() throws Exception {

		new Sort(Direction.ASC, (String[]) null);
	}

	/**
	 * Asserts that the class rejects {@code null} values in the properties array.
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullProperty() throws Exception {

		new Sort(Direction.ASC, (String) null);
	}

	/**
	 * Asserts that the class rejects empty strings in the properties array.
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsEmptyProperty() throws Exception {

		new Sort(Direction.ASC, "");
	}

	/**
	 * Asserts that the class rejects no properties given at all.
	 * 
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNoProperties() throws Exception {

		new Sort(Direction.ASC);
	}

	@Test
	public void allowsCombiningSorts() {

		Sort sort = new Sort("foo").and(new Sort("bar"));
		assertThat(sort, hasItems(new Sort.Order("foo"), new Sort.Order("bar")));
	}

	@Test
	public void handlesAdditionalNullSort() {

		Sort sort = new Sort("foo").and(null);
		assertThat(sort, hasItem(new Sort.Order("foo")));
	}

	/**
	 * @see DATACMNS-281
	 * @author Kevin Raymond
	 */
	@Test
	public void configuresIgnoreCaseForOrder() {
		assertThat(new Order(Direction.ASC, "foo").ignoreCase().isIgnoreCase(), is(true));
	}

	/**
	 * @see DATACMNS-281
	 * @author Kevin Raymond
	 */
	@Test
	public void orderDoesNotIgnoreCaseByDefault() {
		assertThat(new Order(Direction.ASC, "foo").isIgnoreCase(), is(false));
	}

	/**
	 * @see DATACMNS-436
	 */
	@Test
	public void ordersWithDifferentIgnoreCaseDoNotEqual() {

		Order foo = new Order("foo");
		Order fooIgnoreCase = new Order("foo").ignoreCase();

		assertThat(foo, is(not(fooIgnoreCase)));
		assertThat(foo.hashCode(), is(not(fooIgnoreCase.hashCode())));
	}

	/**
	 * @see DATACMNS-491
	 */
	@Test
	public void orderWithNullHandlingHintNullsFirst() {
		assertThat(new Order("foo").nullsFirst().getNullHandling(), is(NULLS_FIRST));
	}

	/**
	 * @see DATACMNS-491
	 */
	@Test
	public void orderWithNullHandlingHintNullsLast() {
		assertThat(new Order("foo").nullsLast().getNullHandling(), is(NULLS_LAST));
	}

	/**
	 * @see DATACMNS-491
	 */
	@Test
	public void orderWithNullHandlingHintNullsNative() {
		assertThat(new Order("foo").nullsNative().getNullHandling(), is(NATIVE));
	}

	/**
	 * @see DATACMNS-491
	 */
	@Test
	public void orderWithDefaultNullHandlingHint() {
		assertThat(new Order("foo").getNullHandling(), is(NATIVE));
	}
}
