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
 * Unit tests for {@link Box}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class BoxUnitTests {

	Box first = new Box(new Point(1d, 1d), new Point(2d, 2d));
	Box second = new Box(new Point(1d, 1d), new Point(2d, 2d));
	Box third = new Box(new Point(3d, 3d), new Point(1d, 1d));

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void equalsWorksCorrectly() {

		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();
		assertThat(first.equals(third)).isFalse();
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void hashCodeWorksCorrectly() {

		assertThat(first.hashCode()).isEqualTo(second.hashCode());
		assertThat(first.hashCode()).isNotEqualTo(third.hashCode());
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void testToString() {

		assertThat(first.toString()).isEqualTo("Box [Point [x=1.000000, y=1.000000], Point [x=2.000000, y=2.000000]]");
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		Box serialized = (Box) SerializationUtils.deserialize(SerializationUtils.serialize(first));
		assertThat(serialized).isEqualTo(first);
	}
}
