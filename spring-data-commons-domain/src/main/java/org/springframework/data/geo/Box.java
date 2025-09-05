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

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Represents a geospatial box value
 *
 * @author Mark Pollack
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Box implements Shape {

	private static final @Serial long serialVersionUID = 8198095179084040711L;

	private final Point first;
	private final Point second;

	/**
	 * Creates a new Box spanning from the given first to the second {@link Point}.
	 *
	 * @param first must not be {@literal null}.
	 * @param second must not be {@literal null}.
	 */
	public Box(Point first, Point second) {

		Assert.notNull(first, "First point must not be null");
		Assert.notNull(second, "Second point must not be null");

		this.first = first;
		this.second = second;
	}

	/**
	 * Creates a new Box from the given {@code first} to the {@code second} point represented as the {@literal double[]}.
	 *
	 * @param first must not be {@literal null} and contain exactly 2 doubles.
	 * @param second must not be {@literal null} and contain exactly 2 doubles.
	 */
	public Box(double[] first, double[] second) {

		Assert.isTrue(first.length == 2, "Point array has to have 2 elements");
		Assert.isTrue(second.length == 2, "Point array has to have 2 elements");

		this.first = new Point(first[0], first[1]);
		this.second = new Point(second[0], second[1]);
	}

	/**
	 * Returns the first {@link Point} making up the {@link Box}.
	 *
	 * @return the first {@link Point} making up the {@link Box}.
	 */
	public Point getFirst() {
		return first;
	}

	/**
	 * Returns the second {@link Point} making up the {@link Box}.
	 *
	 * @return the second {@link Point} making up the {@link Box}.
	 */
	public Point getSecond() {
		return second;
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(first, second);
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Box that)) {
			return false;
		}

		return this.first.equals(that.first) && this.second.equals(that.second);
	}

	@Override
	public String toString() {
		return String.format("Box [%s, %s]", first, second);
	}
}
