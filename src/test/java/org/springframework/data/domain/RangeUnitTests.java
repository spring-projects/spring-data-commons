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
import org.springframework.data.domain.Range.Bound;

/**
 * Unit tests for {@link Range}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10
 */
public class RangeUnitTests {

	@Test(expected = IllegalArgumentException.class) // DATACMNS-651
	public void rejectsNullReferenceValuesForContains() {
		Range.from(Bound.inclusive(10L)).to(Bound.inclusive(20L)).contains(null);
	}

	@Test // DATACMNS-651
	public void excludesLowerBoundIfConfigured() {

		Range<Long> range = Range.from(Bound.exclusive(10L)).to(Bound.inclusive(20L));

		assertThat(range.contains(10L)).isFalse();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();
	}

	@Test // DATACMNS-651
	public void excludesUpperBoundIfConfigured() {

		Range<Long> range = Range.of(Bound.inclusive(10L), Bound.exclusive(20L));

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isFalse();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();
	}

	@Test // DATACMNS-651, DATACMNS-1050
	public void handlesOpenUpperBoundCorrectly() {

		Range<Long> range = Range.of(Bound.inclusive(10L), Bound.unbounded());

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isTrue();
		assertThat(range.getLowerBound().isBounded()).isTrue();
		assertThat(range.getUpperBound().isBounded()).isFalse();
		assertThat(range.toString()).isEqualTo("[10-unbounded");
	}

	@Test // DATACMNS-651, DATACMNS-1050
	public void handlesOpenLowerBoundCorrectly() {

		Range<Long> range = Range.of(Bound.unbounded(), Bound.inclusive(20L));

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isTrue();
		assertThat(range.contains(25L)).isFalse();
		assertThat(range.getLowerBound().isBounded()).isFalse();
		assertThat(range.getUpperBound().isBounded()).isTrue();
	}

	@Test // DATACMNS-1050
	public void createsInclusiveBoundaryCorrectly() {

		Bound<Integer> bound = Bound.inclusive(10);

		assertThat(bound.isInclusive()).isTrue();
		assertThat(bound.getValue()).contains(10);
	}

	@Test // DATACMNS-1050
	public void createsExclusiveBoundaryCorrectly() {

		Bound<Double> bound = Bound.exclusive(10d);

		assertThat(bound.isInclusive()).isFalse();
		assertThat(bound.getValue()).contains(10d);
	}

	@Test // DATACMNS-1050
	public void createsRangeFromBoundariesCorrectly() {

		Bound<Long> lower = Bound.inclusive(10L);
		Bound<Long> upper = Bound.inclusive(20L);

		Range<Long> range = Range.of(lower, upper);

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(21L)).isFalse();
	}

	@Test // DATACMNS-1050
	public void shouldExclusiveBuildRangeLowerFirst() {

		Range<Long> range = Range.from(Bound.exclusive(10L)).to(Bound.exclusive(20L));

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isFalse();
		assertThat(range.contains(11L)).isTrue();
		assertThat(range.contains(19L)).isTrue();
		assertThat(range.contains(20L)).isFalse();
		assertThat(range.contains(21L)).isFalse();
		assertThat(range.toString()).isEqualTo("(10-20)");
	}

	@Test // DATACMNS-1050
	public void shouldBuildRange() {

		Range<Long> range = Range.from(Bound.inclusive(10L)).to(Bound.inclusive(20L));

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(11L)).isTrue();
		assertThat(range.contains(19L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(21L)).isFalse();
		assertThat(range.toString()).isEqualTo("[10-20]");
	}

	@Test // DATACMNS-1050
	public void createsUnboundedRange() {

		Range<Long> range = Range.unbounded();

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.getLowerBound().getValue()).isEmpty();
		assertThat(range.getUpperBound().getValue()).isEmpty();
	}
}
