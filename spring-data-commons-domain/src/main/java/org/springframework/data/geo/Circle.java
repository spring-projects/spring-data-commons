/*
 * Copyright 2010-2025 the original author or authors.
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

import java.io.Serial;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Represents a geospatial circle value
 *
 * @author Mark Pollack
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Circle implements Shape {

	private static final @Serial long serialVersionUID = 5215611530535947924L;

	private final Point center;
	private final Distance radius;

	/**
	 * Creates a new {@link Circle} from the given {@link Point} and radius.
	 *
	 * @param center must not be {@literal null}.
	 * @param radius must not be {@literal null} and it's value greater or equal to zero.
	 */
	@PersistenceCreator
	public Circle(Point center, Distance radius) {

		Assert.notNull(center, "Center point must not be null");
		Assert.notNull(radius, "Radius must not be null");
		Assert.isTrue(radius.getValue() >= 0, "Radius must not be negative");

		this.center = center;
		this.radius = radius;
	}

	/**
	 * Creates a new {@link Circle} from the given {@link Point} and radius.
	 *
	 * @param center must not be {@literal null}.
	 * @param radius's value must be greater or equal to zero.
	 */
	public Circle(Point center, double radius) {
		this(center, new Distance(radius));
	}

	/**
	 * Creates a new {@link Circle} from the given coordinates and radius as {@link Distance} with a
	 * {@link Metrics#NEUTRAL}.
	 *
	 * @param centerX X coordinate of the center point.
	 * @param centerY Y coordinate of the center point.
	 * @param radius must be greater or equal to zero.
	 */
	public Circle(double centerX, double centerY, double radius) {
		this(new Point(centerX, centerY), new Distance(radius));
	}

	/**
	 * Returns the center of the {@link Circle}.
	 *
	 * @return will never be {@literal null}.
	 */
	public Point getCenter() {
		return center;
	}

	/**
	 * Returns the radius of the {@link Circle}.
	 *
	 * @return the radius.
	 */
	public Distance getRadius() {
		return radius;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Circle circle)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(center, circle.center)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(radius, circle.radius);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(center, radius);
	}

	@Override
	public String toString() {
		return String.format("Circle: [center=%s, radius=%s]", center, radius);
	}
}
