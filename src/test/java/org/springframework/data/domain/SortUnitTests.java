/*
 * Copyright 2008-2018 the original author or authors.
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
 * @author Mark Paluch
 */
public class SortUnitTests {

	/**
	 * Asserts that the class applies the default sort order if no order or {@code null} was provided.
	 *
	 * @throws Exception
	 */
	@Test
	public void appliesDefaultForOrder() throws Exception {
		assertThat(Sort.by("foo").iterator().next().getDirection()).isEqualTo(Sort.DEFAULT_DIRECTION);
	}

	/**
	 * Asserts that the class rejects {@code null} as properties array.
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("null")
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullProperties() throws Exception {

		Sort.by(Direction.ASC, (String[]) null);
	}

	/**
	 * Asserts that the class rejects {@code null} values in the properties array.
	 *
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNullProperty() throws Exception {

		Sort.by(Direction.ASC, (String) null);
	}

	/**
	 * Asserts that the class rejects empty strings in the properties array.
	 *
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsEmptyProperty() throws Exception {

		Sort.by(Direction.ASC, "");
	}

	/**
	 * Asserts that the class rejects no properties given at all.
	 *
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void preventsNoProperties() throws Exception {

		Sort.by(Direction.ASC);
	}

	@Test
	public void allowsCombiningSorts() {

		Sort sort = Sort.by("foo").and(Sort.by("bar"));
		assertThat(sort).containsExactly(Order.by("foo"), Order.by("bar"));
	}

	@Test
	public void handlesAdditionalNullSort() {

		Sort sort = Sort.by("foo").and(Sort.unsorted());

		assertThat(sort).containsExactly(Order.by("foo"));
	}

	@Test // DATACMNS-281, DATACMNS-1021
	public void configuresIgnoreCaseForOrder() {
		assertThat(Order.asc("foo").ignoreCase().isIgnoreCase()).isTrue();
	}

	@Test // DATACMNS-281, DATACMNS-1021
	public void orderDoesNotIgnoreCaseByDefault() {

		assertThat(Order.by("foo").isIgnoreCase()).isFalse();
		assertThat(Order.asc("foo").isIgnoreCase()).isFalse();
		assertThat(Order.desc("foo").isIgnoreCase()).isFalse();
	}

	@Test // DATACMNS-1021
	public void createsOrderWithDirection() {

		assertThat(Order.asc("foo").getDirection()).isEqualTo(Direction.ASC);
		assertThat(Order.desc("foo").getDirection()).isEqualTo(Direction.DESC);
	}

	@Test // DATACMNS-436
	public void ordersWithDifferentIgnoreCaseDoNotEqual() {

		Order foo = Order.by("foo");
		Order fooIgnoreCase = Order.by("foo").ignoreCase();

		assertThat(foo).isNotEqualTo(fooIgnoreCase);
		assertThat(foo.hashCode()).isNotEqualTo(fooIgnoreCase.hashCode());
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsFirst() {
		assertThat(Order.by("foo").nullsFirst().getNullHandling()).isEqualTo(NULLS_FIRST);
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsLast() {
		assertThat(Order.by("foo").nullsLast().getNullHandling()).isEqualTo(NULLS_LAST);
	}

	@Test // DATACMNS-491
	public void orderWithNullHandlingHintNullsNative() {
		assertThat(Order.by("foo").nullsNative().getNullHandling()).isEqualTo(NATIVE);
	}

	@Test // DATACMNS-491
	public void orderWithDefaultNullHandlingHint() {
		assertThat(Order.by("foo").getNullHandling()).isEqualTo(NATIVE);
	}

	@Test // DATACMNS-908
	public void createsNewOrderForDifferentProperty() {

		Order source = Order.desc("foo").nullsFirst().ignoreCase();
		Order result = source.withProperty("bar");

		assertThat(result.getProperty()).isEqualTo("bar");
		assertThat(result.getDirection()).isEqualTo(source.getDirection());
		assertThat(result.getNullHandling()).isEqualTo(source.getNullHandling());
		assertThat(result.isIgnoreCase()).isEqualTo(source.isIgnoreCase());
	}

	@Test
	@SuppressWarnings("null")
	public void preventsNullDirection() {

		assertThatExceptionOfType(IllegalArgumentException.class)//
				.isThrownBy(() -> Sort.by((Direction) null, "foo"))//
				.withMessageContaining("Direction");
	}
}
