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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link Polygon}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class PolygonUnitTests {

	Point first = new Point(1, 1);
	Point second = new Point(2, 2);
	Point third = new Point(3, 3);

	/**
	 * @see DATACMNS-437
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPoints() {
		new Polygon(null, null, null);
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void createsSimplePolygon() {

		Polygon polygon = new Polygon(third, second, first);

		assertThat(polygon).isNotNull();
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void isEqualForSamePoints() {

		Polygon left = new Polygon(third, second, first);
		Polygon right = new Polygon(third, second, first);

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void testToString() {

		assertThat(new Polygon(third, second, first).toString()).isEqualTo(
				"Polygon: [Point [x=3.000000, y=3.000000],Point [x=2.000000, y=2.000000],Point [x=1.000000, y=1.000000]]");
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		Polygon polygon = new Polygon(third, second, first);

		Polygon serialized = (Polygon) SerializationUtils.deserialize(SerializationUtils.serialize(polygon));
		assertThat(serialized).isEqualTo(polygon);
	}
}
