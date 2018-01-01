/*
 * Copyright 2014-2018 the original author or authors.
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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Custom module to deserialize the geo-spatial value objects using Jackson 2.
 *
 * @author Oliver Gierke
 * @since 1.8
 */
public class GeoModule extends SimpleModule {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link GeoModule} registering mixins for common geo-spatial types.
	 */
	public GeoModule() {

		super("Spring Data Geo Mixins", new Version(1, 0, 0, null, "org.springframework.data", "spring-data-commons-geo"));

		setMixInAnnotation(Distance.class, DistanceMixin.class);
		setMixInAnnotation(Point.class, PointMixin.class);
		setMixInAnnotation(Box.class, BoxMixin.class);
		setMixInAnnotation(Circle.class, CircleMixin.class);
		setMixInAnnotation(Polygon.class, PolygonMixin.class);
	}

	@JsonIgnoreProperties("unit")
	static abstract class DistanceMixin {

		DistanceMixin(@JsonProperty("value") double value,
				@JsonProperty("metric") @JsonDeserialize(as = Metrics.class) Metric metic) {}

		@JsonIgnore
		abstract double getNormalizedValue();
	}

	static abstract class PointMixin {
		PointMixin(@JsonProperty("x") double x, @JsonProperty("y") double y) {}
	}

	static abstract class CircleMixin {
		CircleMixin(@JsonProperty("center") Point center, @JsonProperty("radius") Distance radius) {}
	}

	static abstract class BoxMixin {
		BoxMixin(@JsonProperty("first") Point first, @JsonProperty("second") Point point) {}
	}

	static abstract class PolygonMixin {
		PolygonMixin(@JsonProperty("points") List<Point> points) {}
	}
}
