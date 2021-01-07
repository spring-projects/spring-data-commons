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
 * Unit tests for {@link GeoResult}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class GeoResultUnitTests {

	GeoResult<String> first = new GeoResult<>("Foo", new Distance(2.5));
	GeoResult<String> second = new GeoResult<>("Foo", new Distance(2.5));
	GeoResult<String> third = new GeoResult<>("Bar", new Distance(2.5));
	GeoResult<String> fourth = new GeoResult<>("Foo", new Distance(5.2));

	@Test // DATACMNS-437
	void considersSameInstanceEqual() {

		assertThat(first.equals(first)).isTrue();
	}

	@Test // DATACMNS-437
	void considersSameValuesAsEqual() {

		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();
		assertThat(first.equals(third)).isFalse();
		assertThat(third.equals(first)).isFalse();
		assertThat(first.equals(fourth)).isFalse();
		assertThat(fourth.equals(first)).isFalse();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	// DATACMNS-437
	void rejectsNullContent() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeoResult(null, new Distance(2.5)));
	}

	@Test // DATACMNS-482
	void testSerialization() {

		GeoResult<String> result = new GeoResult<>("test", new Distance(2));

		@SuppressWarnings("unchecked")
		GeoResult<String> serialized = (GeoResult<String>) SerializationUtils.deserialize(SerializationUtils.serialize(result));
		assertThat(serialized).isEqualTo(result);
	}
}
