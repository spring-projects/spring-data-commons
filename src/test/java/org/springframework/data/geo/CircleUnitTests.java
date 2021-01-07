/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.geo;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link Circle}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class CircleUnitTests {

	@Test // DATACMNS-437
	void rejectsNullOrigin() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Circle(null, new Distance(0)));
	}

	@Test // DATACMNS-437
	void rejectsNegativeRadius() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Circle(1, 1, -1));
	}

	@Test // DATACMNS-437
	void considersTwoCirclesEqualCorrectly() {

		Circle left = new Circle(1, 1, 1);
		Circle right = new Circle(1, 1, 1);

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);

		right = new Circle(new Point(1, 1), new Distance(1));

		assertThat(left).isEqualTo(right);
		assertThat(right).isEqualTo(left);
	}

	@Test // DATACMNS-437
	void testToString() {

		assertThat(new Circle(1, 1, 1).toString()).isEqualTo("Circle: [center=Point [x=1.000000, y=1.000000], radius=1.0]");
	}

	@Test // DATACMNS-482
	void testSerialization() {

		Circle circle = new Circle(1, 1, 1);

		Circle serialized = (Circle) SerializationUtils.deserialize(SerializationUtils.serialize(circle));
		assertThat(serialized).isEqualTo(circle);
	}
}
