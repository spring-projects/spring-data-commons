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
package org.springframework.data.repository.query.parser;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * Unit test for {@link OrderBySource}.
 *
 * @author Oliver Gierke
 */
class OrderBySourceUnitTests {

	@Test
	void handlesSingleDirectionAndPropertyCorrectly() {
		assertThat(new OrderBySource("UsernameDesc").toSort()).isEqualTo(Sort.by("username").descending());
	}

	@Test
	void handlesCamelCasePropertyCorrecty() {
		assertThat(new OrderBySource("LastnameUsernameDesc").toSort()).isEqualTo(Sort.by("lastnameUsername").descending());
	}

	@Test
	void handlesMultipleDirectionsCorrectly() {

		OrderBySource orderBySource = new OrderBySource("LastnameAscUsernameDesc");
		assertThat(orderBySource.toSort()).isEqualTo(Sort.by("lastname").ascending().and(Sort.by("username").descending()));
	}

	@Test
	void rejectsMissingProperty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OrderBySource("Desc"));
	}

	@Test
	void usesNestedPropertyCorrectly() throws Exception {

		OrderBySource source = new OrderBySource("BarNameDesc", Optional.of(Foo.class));
		assertThat(source.toSort()).isEqualTo(Sort.by("bar.name").descending());
	}

	@Test // DATACMNS-641
	void defaultsSortOrderToAscendingSort() {

		OrderBySource source = new OrderBySource("lastname");
		assertThat(source.toSort()).isEqualTo(Sort.by("lastname"));
	}

	@Test
	void orderBySourceFromEmptyStringResultsInUnsorted() {
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
