/*
 * Copyright 2023 the original author or authors.
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

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Window}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class WindowUnitTests {

	@Test // GH-2151
	void equalsAndHashCode() {

		IntFunction<OffsetScrollPosition> positionFunction = OffsetScrollPosition.positionFunction(0);
		Window<Integer> one = Window.from(List.of(1, 2, 3), positionFunction);
		Window<Integer> two = Window.from(List.of(1, 2, 3), positionFunction);

		assertThat(one).isEqualTo(two).hasSameHashCodeAs(two);
		assertThat(one.equals(two)).isTrue();

		assertThat(Window.from(List.of(1, 2, 3), positionFunction, true)).isNotEqualTo(two).doesNotHaveSameHashCodeAs(two);
	}

	@Test // GH-2151
	void allowsIteration() {

		Window<Integer> window = Window.from(List.of(1, 2, 3), OffsetScrollPosition.positionFunction(0));

		for (Integer integer : window) {
			assertThat(integer).isBetween(1, 3);
		}
	}

	@Test // GH-2151
	void shouldCreateCorrectPositions() {

		Window<Integer> window = Window.from(List.of(1, 2, 3), OffsetScrollPosition.positionFunction(0));

		assertThat(window.positionAt(0)).isEqualTo(ScrollPosition.offset(1));
		assertThat(window.positionAt(window.size() - 1)).isEqualTo(ScrollPosition.offset(3));

		// by index
		assertThat(window.positionAt(1)).isEqualTo(ScrollPosition.offset(2));

		// by object
		assertThat(window.positionAt(Integer.valueOf(1))).isEqualTo(ScrollPosition.offset(1));
	}
}
