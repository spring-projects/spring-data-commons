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
import static org.junit.Assert.*;

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
	@SuppressWarnings("unchecked")
	public void calculatesAverageForGivenGeoResults() {

		GeoResult<Object> first = new GeoResult<Object>(new Object(), new Distance(2));
		GeoResult<Object> second = new GeoResult<Object>(new Object(), new Distance(5));
		GeoResults<Object> geoResults = new GeoResults<Object>(Arrays.asList(first, second));

		assertThat(geoResults.getAverageDistance(), is(new Distance(3.5)));
	}

	/**
	 * @see DATACMNS-482
	 */
	@Test
	public void testSerialization() {

		GeoResult<String> result = new GeoResult<String>("test", new Distance(2));
		@SuppressWarnings("unchecked")
		GeoResults<String> geoResults = new GeoResults<String>(Arrays.asList(result));

		@SuppressWarnings("unchecked")
		GeoResults<String> serialized = (GeoResults<String>) SerializationUtils.deserialize(SerializationUtils
				.serialize(geoResults));
		assertThat(serialized, is(geoResults));
	}
}
