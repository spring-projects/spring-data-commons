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
import java.io.Serializable;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Represents a geospatial point value.
 *
 * @author Mark Pollack
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Point implements Serializable {

	private static final @Serial long serialVersionUID = 3583151228933783558L;

	private final double x;
	private final double y;

	/**
	 * Creates a {@link Point} from the given {@code x}, {@code y} coordinate.
	 *
	 * @param x x coordinate.
	 * @param y y coordinate.
	 */
	@PersistenceCreator
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Creates a {@link Point} from the given {@link Point} coordinate.
	 *
	 * @param point must not be {@literal null}.
	 */
	public Point(Point point) {

		Assert.notNull(point, "Source point must not be null");

		this.x = point.x;
		this.y = point.y;
	}

	/**
	 * Returns the x-coordinate of the {@link Point}.
	 *
	 * @return the x-coordinate of the {@link Point}.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Returns the y-coordinate of the {@link Point}.
	 *
	 * @return the y-coordinate of the {@link Point}.
	 */
	public double getY() {
		return y;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(x, y);
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Point other)) {
			return false;
		}

		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) {
			return false;
		}

		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format(Locale.ENGLISH, "Point [x=%f, y=%f]", x, y);
	}
}
