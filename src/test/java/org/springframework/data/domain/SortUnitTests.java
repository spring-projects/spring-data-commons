/*
 * Copyright 2008-2023 the original author or authors.
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
import static org.springframework.data.domain.Sort.NullHandling.*;

import lombok.Getter;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.geo.Circle;

/**
 * Unit test for {@link Sort}.
 *
 * @author Oliver Gierke
 * @author Kevin Raymond
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Hiufung Kwok
 */
class SortUnitTests {

	/**
	 * Asserts that the class applies the default sort order if no order or {@code null} was provided.
	 */
	@Test
	void appliesDefaultForOrder() {
		assertThat(Sort.by("foo").iterator().next().getDirection()).isEqualTo(Sort.DEFAULT_DIRECTION);
	}

	/**
	 * Asserts that the class rejects {@code null} as properties array.
	 */
	@Test
	@SuppressWarnings("null")
	void preventsNullProperties() {
		assertThatIllegalArgumentException().isThrownBy(() -> Sort.by(Direction.ASC, (String[]) null));
	}

	/**
	 * Asserts that the class rejects {@code null} values in the properties array.
	 */
	@Test
	void preventsNullProperty() {
		assertThatIllegalArgumentException().isThrownBy(() -> Sort.by(Direction.ASC, (String) null));
	}

	/**
	 * Asserts that the class rejects empty strings in the properties array.
	 */
	@Test
	void preventsEmptyProperty() {
		assertThatIllegalArgumentException().isThrownBy(() -> Sort.by(Direction.ASC, ""));
	}

	/**
	 * Asserts that the class rejects no properties given at all.
	 */
	@Test
	void preventsNoProperties() {
		assertThatIllegalArgumentException().isThrownBy(() -> Sort.by(Direction.ASC));
	}

	@Test
	void allowsCombiningSorts() {

		var sort = Sort.by("foo").and(Sort.by("bar"));
		assertThat(sort).containsExactly(Order.by("foo"), Order.by("bar"));
	}

	@Test
	void handlesAdditionalNullSort() {

		var sort = Sort.by("foo").and(Sort.unsorted());

		assertThat(sort).containsExactly(Order.by("foo"));
	}

	@Test // DATACMNS-281, DATACMNS-1021, GH-2585
	void configuresIgnoreCaseForOrder() {

		assertThat(Order.asc("foo").ignoreCase().isIgnoreCase()).isTrue();
		assertThat(Sort.by(Order.by("foo").ignoreCase()).descending().iterator().next().isIgnoreCase()).isTrue();
		assertThat(Sort.by(Order.by("foo").ignoreCase()).ascending().iterator().next().isIgnoreCase()).isTrue();
	}

	@Test // DATACMNS-281, DATACMNS-1021
	void orderDoesNotIgnoreCaseByDefault() {

		assertThat(Order.by("foo").isIgnoreCase()).isFalse();
		assertThat(Order.asc("foo").isIgnoreCase()).isFalse();
		assertThat(Order.desc("foo").isIgnoreCase()).isFalse();
	}

	@Test // DATACMNS-1021
	void createsOrderWithDirection() {

		assertThat(Order.asc("foo").getDirection()).isEqualTo(Direction.ASC);
		assertThat(Order.desc("foo").getDirection()).isEqualTo(Direction.DESC);
	}

	@Test // DATACMNS-436
	void ordersWithDifferentIgnoreCaseDoNotEqual() {

		var foo = Order.by("foo");
		var fooIgnoreCase = Order.by("foo").ignoreCase();

		assertThat(foo).isNotEqualTo(fooIgnoreCase);
		assertThat(foo.hashCode()).isNotEqualTo(fooIgnoreCase.hashCode());
	}

	@Test // DATACMNS-491
	void orderWithNullHandlingHintNullsFirst() {
		assertThat(Order.by("foo").nullsFirst().getNullHandling()).isEqualTo(NULLS_FIRST);
	}

	@Test // DATACMNS-491
	void orderWithNullHandlingHintNullsLast() {
		assertThat(Order.by("foo").nullsLast().getNullHandling()).isEqualTo(NULLS_LAST);
	}

	@Test // DATACMNS-491
	void orderWithNullHandlingHintNullsNative() {
		assertThat(Order.by("foo").nullsNative().getNullHandling()).isEqualTo(NATIVE);
	}

	@Test // DATACMNS-491
	void orderWithDefaultNullHandlingHint() {
		assertThat(Order.by("foo").getNullHandling()).isEqualTo(NATIVE);
	}

	@Test // GH-2585
	void retainsNullHandlingAfterChangingDirection() {

		assertThat(Sort.by(Order.by("foo").with(NULLS_FIRST)).ascending().iterator().next().getNullHandling())
				.isEqualTo(NULLS_FIRST);
		assertThat(Sort.by(Order.by("foo").with(NULLS_FIRST)).descending().iterator().next().getNullHandling())
				.isEqualTo(NULLS_FIRST);
	}

	@Test // DATACMNS-908
	void createsNewOrderForDifferentProperty() {

		var source = Order.desc("foo").nullsFirst().ignoreCase();
		var result = source.withProperty("bar");

		assertThat(result.getProperty()).isEqualTo("bar");
		assertThat(result.getDirection()).isEqualTo(source.getDirection());
		assertThat(result.getNullHandling()).isEqualTo(source.getNullHandling());
		assertThat(result.isIgnoreCase()).isEqualTo(source.isIgnoreCase());
	}

	@Test
	@SuppressWarnings("null")
	void preventsNullDirection() {

		assertThatIllegalArgumentException()//
				.isThrownBy(() -> Sort.by((Direction) null, "foo"))//
				.withMessageContaining("Direction");
	}

	@Test // DATACMNS-1450
	void translatesTypedSortCorrectly() {

		assertThat(Sort.sort(Sample.class).by(Sample::getNested).by(Nested::getFirstname)) //
				.containsExactly(Order.by("nested.firstname"));

		assertThat(Sort.sort(Sample.class).by((Sample it) -> it.getNested().getFirstname())) //
				.containsExactly(Order.by("nested.firstname"));

		assertThat(Sort.sort(Sample.class).by(Sample::getNesteds).by(Nested::getFirstname)) //
				.containsExactly(Order.by("nesteds.firstname"));
	}

	@Test // #2103
	void allowsCombiningTypedSorts() {

		assertThat(Sort.sort(Circle.class).by(Circle::getCenter) //
				.and(Sort.sort(Circle.class).by(Circle::getRadius))) //
						.containsExactly(Order.by("center"), Order.by("radius"));

		assertThat(Sort.by("center").and(Sort.sort(Circle.class).by(Circle::getRadius))) //
				.containsExactly(Order.by("center"), Order.by("radius"));
	}

	@Test // GH-2805
	void reversesSortCorrectly() {
		assertThat(Sort.by(Order.asc("center"), Order.desc("radius")).reverse()) //
				.containsExactly(Order.desc("center"), Order.asc("radius"));
	}

	@Test // GH-2805
	void reversesTypedSortCorrectly() {

		Sort reverse = Sort.sort(Circle.class).by(Circle::getCenter).reverse();
		assertThat(reverse) //
				.containsExactly(Order.desc("center"));

	}

	@Getter
	static class Sample {
		Nested nested;
		Collection<Nested> nesteds;
	}

	@Getter
	static class Nested {
		String firstname;
	}
}
