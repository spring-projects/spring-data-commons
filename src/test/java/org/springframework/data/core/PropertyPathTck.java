/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.core;

import static org.assertj.core.api.Assertions.*;

import java.util.Iterator;

/**
 * TCK for {@link PropertyPath} implementations.
 *
 * @author Mark Paluch
 */
class PropertyPathTck {

	/**
	 * Verify that the given {@link PropertyPath} API behavior matches the expected one.
	 *
	 * @param actual
	 * @param expected
	 */
	static void verify(PropertyPath actual, PropertyPath expected) {

		assertThat(actual).hasToString(expected.toString()).hasSameHashCodeAs(expected).isEqualTo(expected);

		assertThat(actual.getSegment()).isEqualTo(expected.getSegment());
		assertThat(actual.getType()).isEqualTo(expected.getType());

		assertThat(actual.getLeafProperty()).isEqualTo(expected.getLeafProperty());

		assertThat(actual.hasNext()).isEqualTo(expected.hasNext());
		assertThat(actual.next()).isEqualTo(expected.next());

		Iterator<PropertyPath> actualIterator = actual.iterator();
		Iterator<PropertyPath> expectedIterator = actual.iterator();

		assertThat(actualIterator.hasNext()).isEqualTo(expectedIterator.hasNext());

		assertThat(actualIterator.next()).isEqualTo(actual);
		assertThat(expectedIterator.next()).isEqualTo(expected);

		while (actualIterator.hasNext() && expectedIterator.hasNext()) {

			verify(actualIterator.next(), expectedIterator.next());
			assertThat(actualIterator.hasNext()).isEqualTo(expectedIterator.hasNext());
		}

		while (actual != null && expected != null && actual.hasNext() && expected.hasNext()) {

			actual = actual.next();
			expected = expected.next();

			verify(actual, expected);
		}
	}

}
