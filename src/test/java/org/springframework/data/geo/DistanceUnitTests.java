/*
 * Copyright 2011-2015 the original author or authors.
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
package org.springframework.data.geo;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.geo.Metrics.*;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.springframework.data.domain.Range;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link Distance}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class DistanceUnitTests {

	private static final Offset<Double> EPS = Offset.offset(0.000000001);
	private static final double TEN_MILES_NORMALIZED = 0.002523219294755161;
	private static final double TEN_KM_NORMALIZED = 0.001567855942887398;

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void defaultsMetricToNeutralOne() {

		assertThat(new Distance(2.5).getMetric()).isEqualTo((Metric) Metrics.NEUTRAL);
		assertThat(new Distance(2.5, null).getMetric()).isEqualTo((Metric) Metrics.NEUTRAL);
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void addsDistancesWithoutExplicitMetric() {

		Distance left = new Distance(2.5, KILOMETERS);
		Distance right = new Distance(2.5, KILOMETERS);

		assertThat(left.add(right)).isEqualTo(new Distance(5.0, KILOMETERS));
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void addsDistancesWithExplicitMetric() {

		Distance left = new Distance(2.5, KILOMETERS);
		Distance right = new Distance(2.5, KILOMETERS);

		assertThat(left.add(right, MILES)).isEqualTo(new Distance(3.106856281073925, MILES));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void distanceWithSameMetricShoudEqualAfterConversion() {

		assertThat(new Distance(1).in(NEUTRAL)).isEqualTo(new Distance(1));
		assertThat(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS)).isEqualTo(new Distance(10, KILOMETERS));
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES)).isEqualTo(new Distance(10, MILES));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void distanceWithDifferentMetricShoudEqualAfterConversion() {

		assertThat(new Distance(10, MILES)).isEqualTo(new Distance(TEN_MILES_NORMALIZED).in(MILES));
		assertThat(new Distance(10, KILOMETERS)).isEqualTo(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void conversionShouldProduceCorrectNormalizedValue() {

		assertThat(new Distance(TEN_KM_NORMALIZED, NEUTRAL).in(KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, KILOMETERS).getNormalizedValue(), EPS);

		assertThat(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, KILOMETERS).getNormalizedValue(), EPS);

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES).getNormalizedValue())
				.isCloseTo(new Distance(10, MILES).getNormalizedValue(), EPS);

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES).getNormalizedValue())
				.isCloseTo(new Distance(16.09344, KILOMETERS).getNormalizedValue(), EPS);

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(KILOMETERS).getNormalizedValue())
				.isCloseTo(new Distance(10, MILES).getNormalizedValue(), EPS);

		assertThat(new Distance(10, KILOMETERS).in(MILES).getNormalizedValue())
				.isCloseTo(new Distance(6.21371192, MILES).getNormalizedValue(), EPS);
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void toStringAfterConversion() {

		assertThat(new Distance(10, KILOMETERS).in(MILES).toString())
				.isEqualTo(new Distance(6.21371256214785, MILES).toString());
		assertThat(new Distance(6.21371256214785, MILES).in(KILOMETERS).toString())
				.isEqualTo(new Distance(10, KILOMETERS).toString());
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		Distance dist = new Distance(10, KILOMETERS);

		Distance serialized = (Distance) SerializationUtils.deserialize(SerializationUtils.serialize(dist));
		assertThat(serialized).isEqualTo(dist);
	}

	/**
	 * @see DATACMNS-626
	 */
	@Test
	public void returnsMetricsAbbreviationAsUnit() {
		assertThat(new Distance(10, KILOMETERS).getUnit()).isEqualTo("km");
	}

	/**
	 * @see DATACMNS-651
	 */
	@Test
	public void createsARangeCorrectly() {

		Distance twoKilometers = new Distance(2, KILOMETERS);
		Distance tenKilometers = new Distance(10, KILOMETERS);

		Range<Distance> range = Distance.between(twoKilometers, tenKilometers);

		assertThat(range).isNotNull();
		assertThat(range.getLowerBound()).isEqualTo(twoKilometers);
		assertThat(range.getUpperBound()).isEqualTo(tenKilometers);
	}

	/**
	 * @see DATACMNS-651
	 */
	@Test
	public void createsARangeFromPiecesCorrectly() {

		Distance twoKilometers = new Distance(2, KILOMETERS);
		Distance tenKilometers = new Distance(10, KILOMETERS);

		Range<Distance> range = Distance.between(2, KILOMETERS, 10, KILOMETERS);

		assertThat(range).isNotNull();
		assertThat(range.getLowerBound()).isEqualTo(twoKilometers);
		assertThat(range.getUpperBound()).isEqualTo(tenKilometers);
	}

	/**
	 * @see DATACMNS-651
	 */
	@Test
	public void implementsComparableCorrectly() {

		Distance twoKilometers = new Distance(2, KILOMETERS);
		Distance tenKilometers = new Distance(10, KILOMETERS);
		Distance tenKilometersInMiles = new Distance(6.21371256214785, MILES);

		assertThat(tenKilometers.compareTo(tenKilometers)).isEqualTo(0);
		assertThat(tenKilometers.compareTo(tenKilometersInMiles)).isEqualTo(0);
		assertThat(tenKilometersInMiles.compareTo(tenKilometers)).isEqualTo(0);

		assertThat(twoKilometers.compareTo(tenKilometers)).isLessThan(0);
		assertThat(tenKilometers.compareTo(twoKilometers)).isGreaterThan(0);
	}
}
