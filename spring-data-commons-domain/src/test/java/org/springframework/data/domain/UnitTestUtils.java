/*
 * Copyright 2010-2025 the original author or authors.
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

/**
 * @author Oliver Gierke
 */
abstract class UnitTestUtils {

	private UnitTestUtils() {

	}

	/**
	 * Asserts that delivered objects both equal each other as well as return the same hash code.
	 *
	 * @param first
	 * @param second
	 */
	static void assertEqualsAndHashcode(Object first, Object second) {

		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}

	/**
	 * Asserts that both objects are not equal to each other and differ in hash code, too.
	 *
	 * @param first
	 * @param second
	 */
	static void assertNotEqualsAndHashcode(Object first, Object second) {

		assertThat(first).isNotEqualTo(second);
		assertThat(second).isNotEqualTo(first);
		assertThat(first.hashCode()).isNotEqualTo(second.hashCode());
	}
}
