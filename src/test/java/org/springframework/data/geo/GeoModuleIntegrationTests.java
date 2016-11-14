/*
 * Copyright 2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link GeoModule}.
 * 
 * @author Oliver Gierke
 */
public class GeoModuleIntegrationTests {

	ObjectMapper mapper;

	@Before
	public void setUp() {

		this.mapper = new ObjectMapper();
		this.mapper.registerModule(new GeoModule());
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void deserializesDistance() throws Exception {

		String json = "{\"value\":10.0,\"metric\":\"KILOMETERS\"}";
		Distance reference = new Distance(10.0, Metrics.KILOMETERS);

		assertThat(mapper.readValue(json, Distance.class)).isEqualTo(reference);
		assertThat(mapper.writeValueAsString(reference)).isEqualTo(json);
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void deserializesPoint() throws Exception {

		String json = "{\"x\":10.0,\"y\":20.0}";
		Point reference = new Point(10.0, 20.0);

		assertThat(mapper.readValue(json, Point.class)).isEqualTo(reference);
		assertThat(mapper.writeValueAsString(reference)).isEqualTo(json);
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void deserializesCircle() throws Exception {

		String json = "{\"center\":{\"x\":10.0,\"y\":20.0},\"radius\":{\"value\":10.0,\"metric\":\"KILOMETERS\"}}";
		Circle reference = new Circle(new Point(10.0, 20.0), new Distance(10, Metrics.KILOMETERS));

		assertThat(mapper.readValue(json, Circle.class)).isEqualTo(reference);
		assertThat(mapper.writeValueAsString(reference)).isEqualTo(json);
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void deserializesBox() throws Exception {

		String json = "{\"first\":{\"x\":1.0,\"y\":2.0},\"second\":{\"x\":2.0,\"y\":3.0}}";
		Box reference = new Box(new Point(1, 2), new Point(2, 3));

		assertThat(mapper.readValue(json, Box.class)).isEqualTo(reference);
		assertThat(mapper.writeValueAsString(reference)).isEqualTo(json);
	}

	/**
	 * @see DATACMNS-475
	 */
	@Test
	public void deserializesPolygon() throws Exception {

		String json = "{\"points\":[{\"x\":1.0,\"y\":2.0},{\"x\":2.0,\"y\":3.0},{\"x\":3.0,\"y\":4.0}]}";
		Polygon reference = new Polygon(new Point(1, 2), new Point(2, 3), new Point(3, 4));

		assertThat(mapper.readValue(json, Polygon.class)).isEqualTo(reference);
		assertThat(mapper.writeValueAsString(reference)).isEqualTo(json);
	}
}
