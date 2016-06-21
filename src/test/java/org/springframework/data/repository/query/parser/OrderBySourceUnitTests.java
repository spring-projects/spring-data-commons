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
package org.springframework.data.repository.query.parser;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.Test;
import org.springframework.data.domain.Sort;

/**
 * Unit test for {@link OrderBySource}.
 * 
 * @author Oliver Gierke
 */
public class OrderBySourceUnitTests {

	@Test
	public void handlesSingleDirectionAndPropertyCorrectly() throws Exception {
		assertThat(new OrderBySource("UsernameDesc").toSort()).isEqualTo(Sort.by("username").descending());
	}

	@Test
	public void handlesCamelCasePropertyCorrecty() throws Exception {
		assertThat(new OrderBySource("LastnameUsernameDesc").toSort()).isEqualTo(Sort.by("lastnameUsername").descending());
	}

	@Test
	public void handlesMultipleDirectionsCorrectly() throws Exception {

		OrderBySource orderBySource = new OrderBySource("LastnameAscUsernameDesc");
		assertThat(orderBySource.toSort()).isEqualTo(Sort.by("lastname").ascending().and(Sort.by("username").descending()));
		// assertThat(orderBySource.toSort()).hasValue(new Sort(new Order(ASC, "lastname"), new Order(DESC, "username")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMissingProperty() throws Exception {

		new OrderBySource("Desc");
	}

	@Test
	public void usesNestedPropertyCorrectly() throws Exception {

		OrderBySource source = new OrderBySource("BarNameDesc", Optional.of(Foo.class));
		assertThat(source.toSort()).isEqualTo(Sort.by("bar.name").descending());
	}

	/**
	 * @see DATACMNS-641
	 */
	@Test
	public void defaultsSortOrderToAscendingSort() {

		OrderBySource source = new OrderBySource("lastname");
		assertThat(source.toSort()).isEqualTo(Sort.by("lastname"));
	}

	@Test
	public void orderBySourceFromEmptyStringResultsInUnsorted() {
		assertThat(new OrderBySource("").toSort()).isEqualTo(Sort.unsorted());
	}

	@SuppressWarnings("unused")
	private class Foo {

		private Bar bar;
	}

	@SuppressWarnings("unused")
	private class Bar {

		private String name;
	}
}
