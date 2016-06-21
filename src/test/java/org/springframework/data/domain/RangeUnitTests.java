/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

/**
 * Unit tests for {@link Range}.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
public class RangeUnitTests {

	@Test(expected = IllegalArgumentException.class) // DATACMNS-651
	public void rejectsNullReferenceValuesForContains() {
		new Range<>(10L, 20L).contains(null);
	}

	@Test // DATACMNS-651
	public void usesBoundsInclusivelyByDefault() {

		Range<Long> range = new Range<>(10L, 20L);

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();
	}

	@Test // DATACMNS-651
	public void excludesLowerBoundIfConfigured() {

		Range<Long> range = new Range<>(10L, 20L, false, true);

		assertThat(range.contains(10L)).isFalse();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();
	}

	@Test // DATACMNS-651
	public void excludesUpperBoundIfConfigured() {

		Range<Long> range = new Range<>(10L, 20L, true, false);

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isFalse();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();
	}

	@Test // DATACMNS-651
	public void handlesOpenUpperBoundCorrectly() {

		Range<Long> range = new Range<>(10L, null);

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isTrue();
	}

	@Test // DATACMNS-651
	public void handlesOpenLowerBoundCorrectly() {

		Range<Long> range = new Range<>(null, 20L);

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isTrue();
		assertThat(range.contains(25L)).isFalse();
	}
}
