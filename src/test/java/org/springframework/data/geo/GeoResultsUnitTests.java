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

import java.util.Arrays;

import org.junit.Test;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link GeoResults}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class GeoResultsUnitTests {

	/**
	 * @see DATACMNS-437
	 */
	@Test
	public void calculatesAverageForGivenGeoResults() {

		GeoResult<Object> first = new GeoResult<>(new Object(), new Distance(2));
		GeoResult<Object> second = new GeoResult<>(new Object(), new Distance(5));
		GeoResults<Object> geoResults = new GeoResults<>(Arrays.asList(first, second));

		assertThat(geoResults.getAverageDistance()).isEqualTo(new Distance(3.5));
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		GeoResult<String> result = new GeoResult<>("test", new Distance(2));
		GeoResults<String> geoResults = new GeoResults<>(Arrays.asList(result));

		@SuppressWarnings("unchecked")
		GeoResults<String> serialized = (GeoResults<String>) SerializationUtils
				.deserialize(SerializationUtils.serialize(geoResults));
		assertThat(serialized).isEqualTo(geoResults);
	}
}
