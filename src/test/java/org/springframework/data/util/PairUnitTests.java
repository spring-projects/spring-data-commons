/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Pair}.
 *
 * @author Oliver Gierke
 */
class PairUnitTests {

	@Test // DATACMNS-790
	void setsUpSimpleInstance() {

		Pair<Integer, Integer> pair = Pair.of(1, 2);

		assertThat(pair.getFirst()).isEqualTo(1);
		assertThat(pair.getSecond()).isEqualTo(2);
	}

	@Test // DATACMNS-790
	void rejectsNullFirstElement() {
		assertThatIllegalArgumentException().isThrownBy(() -> Pair.of(null, 1));
	}

	@Test // DATACMNS-790
	void rejectsNullSecondElement() {
		assertThatIllegalArgumentException().isThrownBy(() -> Pair.of(1, null));
	}

	@Test // DATACMNS-790
	void hasCorrectEquals() {

		Pair<Integer, Integer> first = Pair.of(1, 2);
		Pair<Integer, Integer> second = Pair.of(1, 2);

		assertThat(first).isEqualTo(first);
		assertThat(first).isEqualTo(second);
		assertThat(second).isEqualTo(first);
	}

	@Test // DATACMNS-790
	void hasCorrectHashCode() {

		Pair<Integer, Integer> first = Pair.of(1, 2);
		Pair<Integer, Integer> second = Pair.of(1, 2);
		Pair<Integer, Integer> third = Pair.of(2, 2);

		assertThat(first.hashCode()).isEqualTo(second.hashCode());
		assertThat(first.hashCode()).isNotEqualTo(third.hashCode());
	}
}
