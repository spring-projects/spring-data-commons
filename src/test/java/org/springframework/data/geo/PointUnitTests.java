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
 * Unit tests for {@link Point}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class PointUnitTests {

	/**
	 * @see DATACMNS-437
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullforCopyConstructor() {
		new Point(null);
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void equalsIsImplementedCorrectly() {

		assertThat(new Point(1.5, 1.5)).isEqualTo(new Point(1.5, 1.5));
		assertThat(new Point(1.5, 1.5)).isNotEqualTo(new Point(2.0, 2.0));
		assertThat(new Point(2.0, 2.0)).isNotEqualTo(new Point(1.5, 1.5));
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void invokingToStringWorksCorrectly() {
		assertThat(new Point(1.5, 1.5).toString()).isEqualTo("Point [x=1.500000, y=1.500000]");
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		Point point = new Point(1.5, 1.5);

		Point serialized = (Point) SerializationUtils.deserialize(SerializationUtils.serialize(point));
		assertThat(serialized).isEqualTo(point);
	}
}
