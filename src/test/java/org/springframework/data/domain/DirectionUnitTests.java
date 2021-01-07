/*
 * Copyright 2010-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Direction;

/**
 * Unit test for {@link Direction}.
 *
 * @author Oliver Gierke
 */
class DirectionUnitTests {

	@Test
	void jpaValueMapping() throws Exception {

		assertThat(Direction.fromString("asc")).isEqualTo(Direction.ASC);
		assertThat(Direction.fromString("desc")).isEqualTo(Direction.DESC);
	}

	@Test
	void rejectsInvalidString() {
		assertThatIllegalArgumentException().isThrownBy(() -> Direction.fromString("foo"));
	}
}
