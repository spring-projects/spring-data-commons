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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

/**
 * Unit tests for {@link GeoResults}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
class GeoResultsUnitTests {

	@Test // DATACMNS-437
	@SuppressWarnings("unchecked")
	void calculatesAverageForGivenGeoResults() {

		var first = new GeoResult<Object>(new Object(), new Distance(2));
		var second = new GeoResult<Object>(new Object(), new Distance(5));
		var geoResults = new GeoResults<Object>(Arrays.asList(first, second));

		assertThat(geoResults.getAverageDistance()).isEqualTo(new Distance(3.5));
	}

	@Test // DATACMNS-482
	void testSerialization() {

		var result = new GeoResult<String>("test", new Distance(2));
		var geoResults = new GeoResults<String>(Collections.singletonList(result));

		@SuppressWarnings("unchecked")
		var serialized = (GeoResults<String>) SerializationUtils
				.deserialize(SerializationUtils.serialize(geoResults));
		assertThat(serialized).isEqualTo(geoResults);
	}
}
