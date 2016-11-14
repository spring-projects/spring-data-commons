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
 * Unit tests for {@link GeoResult}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class GeoResultUnitTests {

	GeoResult<String> first = new GeoResult<String>("Foo", new Distance(2.5));
	GeoResult<String> second = new GeoResult<String>("Foo", new Distance(2.5));
	GeoResult<String> third = new GeoResult<String>("Bar", new Distance(2.5));
	GeoResult<String> fourth = new GeoResult<String>("Foo", new Distance(5.2));

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void considersSameInstanceEqual() {

		assertThat(first.equals(first)).isTrue();
	}

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void considersSameValuesAsEqual() {

		assertThat(first.equals(second)).isTrue();
		assertThat(second.equals(first)).isTrue();
		assertThat(first.equals(third)).isFalse();
		assertThat(third.equals(first)).isFalse();
		assertThat(first.equals(fourth)).isFalse();
		assertThat(fourth.equals(first)).isFalse();
	}

	/**
	 * @see DATACMNS-437
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullContent() {
		new GeoResult(null, new Distance(2.5));
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		GeoResult<String> result = new GeoResult<String>("test", new Distance(2));

		@SuppressWarnings("unchecked")
		GeoResult<String> serialized = (GeoResult<String>) SerializationUtils.deserialize(SerializationUtils.serialize(result));
		assertThat(serialized).isEqualTo(result);
	}
}
