/*
 * Copyright 2008-2017 the original author or authors.
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

	@Test // DATACMNS-281
	public void configuresIgnoreCaseForOrder() {
		assertThat(new Order(Direction.ASC, "foo").ignoreCase().isIgnoreCase(), is(true));
	}

	@Test // DATACMNS-281
	public void orderDoesNotIgnoreCaseByDefault() {
		assertThat(new Order(Direction.ASC, "foo").isIgnoreCase(), is(false));
	}

	@Test // DATACMNS-436
	public void ordersWithDifferentIgnoreCaseDoNotEqual() {

		Order foo = new Order("foo");
		Order fooIgnoreCase = new Order("foo").ignoreCase();

		assertThat(foo, is(not(fooIgnoreCase)));
		assertThat(foo.hashCode(), is(not(fooIgnoreCase.hashCode())));
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsFirst() {
		assertThat(new Order("foo").nullsFirst().getNullHandling(), is(NULLS_FIRST));
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsLast() {
		assertThat(new Order("foo").nullsLast().getNullHandling(), is(NULLS_LAST));
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsNative() {
		assertThat(new Order("foo").nullsNative().getNullHandling(), is(NATIVE));
	}

	@Test // DATACMNS-491
	public void orderWithDefaultNullHandlingHint() {
		assertThat(new Order("foo").getNullHandling(), is(NATIVE));
	}

	@Test // DATACMNS-908
	public void createsNewOrderForDifferentProperty() {

		Order source = new Order(Direction.DESC, "foo").nullsFirst().ignoreCase();
		Order result = source.withProperty("bar");

		assertThat(result.getProperty(), is("bar"));
		assertThat(result.getDirection(), is(source.getDirection()));
		assertThat(result.getNullHandling(), is(source.getNullHandling()));
		assertThat(result.isIgnoreCase(), is(source.isIgnoreCase()));
	}
}
