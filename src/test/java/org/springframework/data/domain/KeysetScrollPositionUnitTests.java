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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.KeysetScrollPosition.Direction;

/**
 * Unit tests for {@link KeysetScrollPosition}.
 *
 * @author Mark Paluch
 */
class KeysetScrollPositionUnitTests {

	@Test // GH-2151
	void equalsAndHashCode() {

		KeysetScrollPosition foo1 = KeysetScrollPosition.of(Collections.singletonMap("k", "v"));
		KeysetScrollPosition foo2 = KeysetScrollPosition.of(Collections.singletonMap("k", "v"));
		KeysetScrollPosition bar = KeysetScrollPosition.of(Collections.singletonMap("k", "v"), Direction.Backward);

		assertThat(foo1).isEqualTo(foo2).hasSameClassAs(foo2);
		assertThat(foo1).isNotEqualTo(bar).doesNotHaveSameHashCodeAs(bar);
	}

}
