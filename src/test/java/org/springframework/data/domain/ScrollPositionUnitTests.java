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
import static org.springframework.data.domain.OffsetScrollPosition.*;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.ScrollPosition.Direction;

/**
 * Unit tests for {@link KeysetScrollPosition}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
class ScrollPositionUnitTests {

	private static final Map<String, ?> KEYS = Collections.singletonMap("k", "v");

	@Test // GH-2151
	void equalsAndHashCodeForKeysets() {

		ScrollPosition foo1 = ScrollPosition.forward(KEYS);
		ScrollPosition foo2 = ScrollPosition.forward(KEYS);
		ScrollPosition bar = ScrollPosition.backward(KEYS);

		assertThat(foo1).isEqualTo(foo2).hasSameClassAs(foo2);
		assertThat(foo1).isNotEqualTo(bar).doesNotHaveSameHashCodeAs(bar);
	}

	@Test // GH-2151
	void equalsAndHashCodeForOffsets() {

		ScrollPosition foo1 = ScrollPosition.offset(1);
		ScrollPosition foo2 = ScrollPosition.offset(1);
		ScrollPosition bar = ScrollPosition.offset(2);

		assertThat(foo1).isEqualTo(foo2).hasSameClassAs(foo2);
		assertThat(foo1).isNotEqualTo(bar).doesNotHaveSameHashCodeAs(bar);
	}

	@Test // GH-2151
	void shouldCreateCorrectIndexPosition() {

		assertThat(positionFunction(0).apply(0)).isEqualTo(ScrollPosition.offset(1));
		assertThat(positionFunction(0).apply(1)).isEqualTo(ScrollPosition.offset(2));

		assertThat(positionFunction(100).apply(0)).isEqualTo(ScrollPosition.offset(101));
		assertThat(positionFunction(100).apply(1)).isEqualTo(ScrollPosition.offset(102));
	}

	@Test // GH-2151
	void rejectsNegativeOffset() {
		assertThatIllegalArgumentException().isThrownBy(() -> ScrollPosition.offset(-1));
	}

	@Test // GH-2151
	void advanceOffsetBelowZeroCapsAtZero() {

		OffsetScrollPosition offset = ScrollPosition.offset(5);

		assertThat(offset.getOffset()).isEqualTo(5);
		assertThat(offset.advanceBy(-10)).isEqualTo(ScrollPosition.offset(0));
	}

	@Test // GH-2824
	void setsUpForwardScrolling() {

		KeysetScrollPosition position = ScrollPosition.forward(KEYS);

		assertThat(position.getKeys()).isEqualTo(KEYS);
		assertThat(position.getDirection()).isEqualTo(Direction.FORWARD);
		assertThat(position.scrollsForward()).isTrue();
		assertThat(position.scrollsBackward()).isFalse();

		KeysetScrollPosition backward = position.backward();

		assertThat(backward.getKeys()).isEqualTo(KEYS);
		assertThat(backward.getDirection()).isEqualTo(Direction.BACKWARD);
		assertThat(backward.scrollsForward()).isFalse();
		assertThat(backward.scrollsBackward()).isTrue();

		assertThat(position.reverse()).isEqualTo(backward);
	}

	@Test // GH-2824
	void setsUpBackwardScrolling() {

		KeysetScrollPosition position = ScrollPosition.backward(KEYS);

		assertThat(position.getKeys()).isEqualTo(KEYS);
		assertThat(position.getDirection()).isEqualTo(Direction.BACKWARD);
		assertThat(position.scrollsForward()).isFalse();
		assertThat(position.scrollsBackward()).isTrue();

		KeysetScrollPosition forward = position.forward();

		assertThat(forward.getKeys()).isEqualTo(KEYS);
		assertThat(forward.getDirection()).isEqualTo(Direction.FORWARD);
		assertThat(forward.scrollsForward()).isTrue();
		assertThat(forward.scrollsBackward()).isFalse();

		assertThat(position.reverse()).isEqualTo(forward);
	}

	@Test // GH-2824
	void initialOffsetPosition() {

		OffsetScrollPosition position = ScrollPosition.offset();

		assertThat(position.isInitial()).isTrue();
		assertThat(position.getOffset()).isEqualTo(0);
	}

	@Test // GH-2824
	void initialKeysetPosition() {

		KeysetScrollPosition keyset = ScrollPosition.keyset();

		assertThat(keyset.isInitial()).isTrue();
		assertThat(keyset.scrollsForward()).isTrue();
	}
}
