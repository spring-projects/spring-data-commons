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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OffsetScrollPosition}.
 *
 * @author Mark Paluch
 */
class OffsetScrollPositionUnitTests {

	@Test // GH-2151
	void equalsAndHashCode() {

		OffsetScrollPosition foo1 = OffsetScrollPosition.of(1);
		OffsetScrollPosition foo2 = OffsetScrollPosition.of(1);
		OffsetScrollPosition bar = OffsetScrollPosition.of(2);

		assertThat(foo1).isEqualTo(foo2).hasSameClassAs(foo2);
		assertThat(foo1).isNotEqualTo(bar).doesNotHaveSameHashCodeAs(bar);
	}

	@Test // GH-2151
	void shouldCreateCorrectIndexPosition() {

		assertThat(positionFunction(0).apply(0)).isEqualTo(OffsetScrollPosition.of(1));
		assertThat(positionFunction(0).apply(1)).isEqualTo(OffsetScrollPosition.of(2));

		assertThat(positionFunction(100).apply(0)).isEqualTo(OffsetScrollPosition.of(101));
		assertThat(positionFunction(100).apply(1)).isEqualTo(OffsetScrollPosition.of(102));
	}
}
