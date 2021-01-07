/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Streamable}
 *
 * @author Oliver Gierke
 * @soundtrack The Intersphere - Antitype (The Grand Delusion)
 */
public class StreamableUnitTests {

	@Test // DATACMNS-1432
	public void collectsToCollections() {

		Streamable<Integer> streamable = Streamable.of(() -> Stream.of(1, 2, 1));

		assertThat(streamable.toList()).containsExactly(1, 2, 1);
		assertThat(streamable.toSet()).containsExactlyInAnyOrder(1, 2);
	}

	@Test // DATACMNS-1433
	public void concatenatesIterable() {
		assertThat(Streamable.of(1, 2).and(Arrays.asList(3, 4))).containsExactly(1, 2, 3, 4);
	}

	@Test // DATACMNS-1433
	public void concatenatesVarargs() {
		assertThat(Streamable.of(1, 2).and(3, 4)).containsExactly(1, 2, 3, 4);
	}

	@Test // DATACMNS-1433
	public void concatenatesStreamable() {
		assertThat(Streamable.of(1, 2).and(Streamable.of(3, 4))).containsExactly(1, 2, 3, 4);
	}

	@Test // DATACMNS-1447
	public void usesStreamableCollectors() {

		assertThat(Streamable.of(1, 2).stream() //
				.collect(Streamable.toStreamable())) //
						.containsExactly(1, 2);

		assertThat(Streamable.of(1, 2, 2).stream() //
				.collect(Streamable.toStreamable(Collectors.toSet()))) //
						.containsExactlyInAnyOrder(1, 2);
	}
}
