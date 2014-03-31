/*
 * Copyright 2011-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.IsCloseTo.*;
import static org.junit.Assert.*;
import static org.springframework.data.geo.Metrics.*;

import org.junit.Test;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link Distance}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class DistanceUnitTests {

	private static final double EPS = 0.000000001;
	private static final double TEN_MILES_NORMALIZED = 0.002523219294755161;
	private static final double TEN_KM_NORMALIZED = 0.001567855942887398;

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void defaultsMetricToNeutralOne() {

		assertThat(new Distance(2.5).getMetric(), is((Metric) Metrics.NEUTRAL));
		assertThat(new Distance(2.5, null).getMetric(), is((Metric) Metrics.NEUTRAL));
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void addsDistancesWithoutExplicitMetric() {

		Distance left = new Distance(2.5, KILOMETERS);
		Distance right = new Distance(2.5, KILOMETERS);

		assertThat(left.add(right), is(new Distance(5.0, KILOMETERS)));
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void addsDistancesWithExplicitMetric() {

		Distance left = new Distance(2.5, KILOMETERS);
		Distance right = new Distance(2.5, KILOMETERS);

		assertThat(left.add(right, MILES), is(new Distance(3.106856281073925, MILES)));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void distanceWithSameMetricShoudEqualAfterConversion() {

		assertThat(new Distance(1).in(NEUTRAL), is(new Distance(1)));
		assertThat(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS), is(new Distance(10, KILOMETERS)));
		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES), is(new Distance(10, MILES)));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void distanceWithDifferentMetricShoudEqualAfterConversion() {

		assertThat(new Distance(10, MILES), is(new Distance(TEN_MILES_NORMALIZED).in(MILES)));
		assertThat(new Distance(10, KILOMETERS), is(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS)));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void conversionShouldProduceCorrectNormalizedValue() {

		assertThat(new Distance(TEN_KM_NORMALIZED, NEUTRAL).in(KILOMETERS).getNormalizedValue(),
				closeTo(new Distance(10, KILOMETERS).getNormalizedValue(), EPS));

		assertThat(new Distance(TEN_KM_NORMALIZED).in(KILOMETERS).getNormalizedValue(),
				closeTo(new Distance(10, KILOMETERS).getNormalizedValue(), EPS));

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES).getNormalizedValue(),
				closeTo(new Distance(10, MILES).getNormalizedValue(), EPS));

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(MILES).getNormalizedValue(),
				closeTo(new Distance(16.09344, KILOMETERS).getNormalizedValue(), EPS));

		assertThat(new Distance(TEN_MILES_NORMALIZED).in(KILOMETERS).getNormalizedValue(),
				closeTo(new Distance(10, MILES).getNormalizedValue(), EPS));

		assertThat(new Distance(10, KILOMETERS).in(MILES).getNormalizedValue(),
				closeTo(new Distance(6.21371192, MILES).getNormalizedValue(), EPS));
	}

	/**
	 * @see DATACMNS-474
	 */
	@Test
	public void toStringAfterConversion() {

		assertThat(new Distance(10, KILOMETERS).in(MILES).toString(), is(new Distance(6.21371256214785, MILES).toString()));
		assertThat(new Distance(6.21371256214785, MILES).in(KILOMETERS).toString(),
				is(new Distance(10, KILOMETERS).toString()));
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		Distance dist = new Distance(10, KILOMETERS);

		Distance serialized = (Distance) SerializationUtils.deserialize(SerializationUtils.serialize(dist));
		assertThat(serialized, is(dist));
	}
}
