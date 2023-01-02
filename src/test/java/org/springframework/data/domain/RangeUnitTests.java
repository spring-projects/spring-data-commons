/*
 * Copyright 2015-2023 the original author or authors.
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

import org.springframework.data.domain.Range.Bound;

/**
 * Unit tests for {@link Range}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10
 */
class RangeUnitTests {

	@Test // DATACMNS-651
	void rejectsNullReferenceValuesForContains() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> Range.from(Bound.inclusive(10L)).to(Bound.inclusive(20L)).contains(null));
	}

	@Test // DATACMNS-651, GH-2571
	void excludesLowerBoundIfConfigured() {

		var range = Range.from(Bound.exclusive(10L)).to(Bound.inclusive(20L));

		assertThat(range.contains(10L)).isFalse();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();

		assertThat(range.contains(10L, Long::compareTo)).isFalse();
		assertThat(range.contains(20L, Long::compareTo)).isTrue();
		assertThat(range.contains(15L, Long::compareTo)).isTrue();
		assertThat(range.contains(5L, Long::compareTo)).isFalse();
		assertThat(range.contains(25L, Long::compareTo)).isFalse();
	}

	@Test // DATACMNS-651, GH-2571
	void excludesUpperBoundIfConfigured() {

		var range = Range.of(Bound.inclusive(10L), Bound.exclusive(20L));

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isFalse();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isFalse();

		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isFalse();
		assertThat(range.contains(15L, Long::compareTo)).isTrue();
		assertThat(range.contains(5L, Long::compareTo)).isFalse();
		assertThat(range.contains(25L, Long::compareTo)).isFalse();
	}

	@Test // DATACMNS-651, DATACMNS-1050, GH-2571
	void handlesOpenUpperBoundCorrectly() {

		var range = Range.of(Bound.inclusive(10L), Bound.unbounded());

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(25L)).isTrue();

		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isTrue();
		assertThat(range.contains(15L, Long::compareTo)).isTrue();
		assertThat(range.contains(5L, Long::compareTo)).isFalse();
		assertThat(range.contains(25L, Long::compareTo)).isTrue();

		assertThat(range.getLowerBound().isBounded()).isTrue();
		assertThat(range.getUpperBound().isBounded()).isFalse();
		assertThat(range.toString()).isEqualTo("[10-unbounded");
	}

	@Test // DATACMNS-651, DATACMNS-1050, GH-2571
	void handlesOpenLowerBoundCorrectly() {

		var range = Range.of(Bound.unbounded(), Bound.inclusive(20L));

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(15L)).isTrue();
		assertThat(range.contains(5L)).isTrue();
		assertThat(range.contains(25L)).isFalse();

		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isTrue();
		assertThat(range.contains(15L, Long::compareTo)).isTrue();
		assertThat(range.contains(5L, Long::compareTo)).isTrue();
		assertThat(range.contains(25L, Long::compareTo)).isFalse();
		assertThat(range.getLowerBound().isBounded()).isFalse();
		assertThat(range.getUpperBound().isBounded()).isTrue();
	}


	@Test // DATACMNS-1050
	void createsInclusiveBoundaryCorrectly() {

		var bound = Bound.inclusive(10);

		assertThat(bound.isInclusive()).isTrue();
		assertThat(bound.getValue()).contains(10);
	}

	@Test // DATACMNS-1050
	void createsExclusiveBoundaryCorrectly() {

		var bound = Bound.exclusive(10d);

		assertThat(bound.isInclusive()).isFalse();
		assertThat(bound.getValue()).contains(10d);
	}

	@Test // DATACMNS-1050, GH-2571
	void createsRangeFromBoundariesCorrectly() {

		var lower = Bound.inclusive(10L);
		var upper = Bound.inclusive(20L);

		var range = Range.of(lower, upper);

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(21L)).isFalse();

		assertThat(range.contains(9L, Long::compareTo)).isFalse();
		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isTrue();
		assertThat(range.contains(21L, Long::compareTo)).isFalse();
	}

	@Test // DATACMNS-1050, GH-2571
	void shouldExclusiveBuildRangeLowerFirst() {

		var range = Range.from(Bound.exclusive(10L)).to(Bound.exclusive(20L));

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isFalse();
		assertThat(range.contains(11L)).isTrue();
		assertThat(range.contains(19L)).isTrue();
		assertThat(range.contains(20L)).isFalse();
		assertThat(range.contains(21L)).isFalse();

		assertThat(range.contains(9L, Long::compareTo)).isFalse();
		assertThat(range.contains(10L, Long::compareTo)).isFalse();
		assertThat(range.contains(11L, Long::compareTo)).isTrue();
		assertThat(range.contains(19L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isFalse();
		assertThat(range.contains(21L, Long::compareTo)).isFalse();
		assertThat(range.toString()).isEqualTo("(10-20)");
	}

	@Test // DATACMNS-1050, GH-2571
	void shouldBuildRange() {

		var range = Range.from(Bound.inclusive(10L)).to(Bound.inclusive(20L));

		assertThat(range.contains(9L)).isFalse();
		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(11L)).isTrue();
		assertThat(range.contains(19L)).isTrue();
		assertThat(range.contains(20L)).isTrue();
		assertThat(range.contains(21L)).isFalse();

		assertThat(range.contains(9L, Long::compareTo)).isFalse();
		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.contains(11L, Long::compareTo)).isTrue();
		assertThat(range.contains(19L, Long::compareTo)).isTrue();
		assertThat(range.contains(20L, Long::compareTo)).isTrue();
		assertThat(range.contains(21L, Long::compareTo)).isFalse();
		assertThat(range.toString()).isEqualTo("[10-20]");
	}

	@Test // DATACMNS-1050, GH-2571
	void createsUnboundedRange() {

		Range<Long> range = Range.unbounded();

		assertThat(range.contains(10L)).isTrue();
		assertThat(range.contains(10L, Long::compareTo)).isTrue();
		assertThat(range.getLowerBound().getValue()).isEmpty();
		assertThat(range.getUpperBound().getValue()).isEmpty();
	}

	@Test // DATACMNS-1499, GH-2571
	void createsOpenRange() {

		var range = Range.open(5L, 10L);

		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(10L)).isFalse();

		assertThat(range.contains(5L, Long::compareTo)).isFalse();
		assertThat(range.contains(10L, Long::compareTo)).isFalse();
	}

	@Test // DATACMNS-1499, GH-2571
	void createsClosedRange() {

		var range = Range.closed(5L, 10L);

		assertThat(range.contains(5L)).isTrue();
		assertThat(range.contains(10L)).isTrue();

		assertThat(range.contains(5L, Long::compareTo)).isTrue();
		assertThat(range.contains(10L, Long::compareTo)).isTrue();
	}

	@Test // DATACMNS-1499, GH-2571
	void createsLeftOpenRange() {

		var range = Range.leftOpen(5L, 10L);

		assertThat(range.contains(5L)).isFalse();
		assertThat(range.contains(10L)).isTrue();

		assertThat(range.contains(5L, Long::compareTo)).isFalse();
		assertThat(range.contains(10L, Long::compareTo)).isTrue();
	}

	@Test // DATACMNS-1499, GH-2571
	void createsRightOpenRange() {

		var range = Range.rightOpen(5L, 10L);

		assertThat(range.contains(5L)).isTrue();
		assertThat(range.contains(10L)).isFalse();

		assertThat(range.contains(5L, Long::compareTo)).isTrue();
		assertThat(range.contains(10L, Long::compareTo)).isFalse();
	}

	@Test // GH-2692
	void mapsBoundaryValues() {

		var range = Range.leftOpen(5L, 10L).map(it -> it * 10);

		assertThat(range.getLowerBound()).isEqualTo(Bound.exclusive(50L));
		assertThat(range.getUpperBound()).isEqualTo(Bound.inclusive(100L));

		range = Range.leftOpen(5L, 10L).map(it -> null);

		assertThat(range.getLowerBound()).isEqualTo(Bound.unbounded());
		assertThat(range.getUpperBound()).isEqualTo(Bound.unbounded());
	}

	@Test // DATACMNS-1499
	void createsLeftUnboundedRange() {
		assertThat(Range.leftUnbounded(Bound.inclusive(10L)).contains(-10000L)).isTrue();
	}

	@Test // DATACMNS-1499
	void createsRightUnboundedRange() {
		assertThat(Range.rightUnbounded(Bound.inclusive(10L)).contains(10000L)).isTrue();
	}

	@Test // DATACMNS-1499
	void createsSingleValueRange() {
		assertThat(Range.just(10L).contains(10L)).isTrue();
	}
}
