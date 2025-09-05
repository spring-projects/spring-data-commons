/*
 * Copyright 2011-2025 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Simple value object to represent a {@link Polygon}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Polygon implements Iterable<Point>, Shape {

	private static final @Serial long serialVersionUID = -2705040068154648988L;

	private final List<Point> points;

	/**
	 * Creates a new {@link Polygon} for the given Points.
	 *
	 * @param x must not be {@literal null}.
	 * @param y must not be {@literal null}.
	 * @param z must not be {@literal null}.
	 * @param others other points.
	 */
	public Polygon(Point x, Point y, Point z, Point... others) {

		Assert.notNull(x, "X coordinate must not be null");
		Assert.notNull(y, "Y coordinate must not be null");
		Assert.notNull(z, "Z coordinate must not be null");
		Assert.notNull(others, "Others must not be null");

		List<Point> points = new ArrayList<>(3 + others.length);
		points.addAll(Arrays.asList(x, y, z));
		points.addAll(Arrays.asList(others));

		this.points = Collections.unmodifiableList(points);
	}

	/**
	 * Creates a new {@link Polygon} for the given Points.
	 *
	 * @param points must not be {@literal null}.
	 */
	@PersistenceCreator
	public Polygon(List<? extends Point> points) {

		Assert.notNull(points, "Points must not be null");

		List<Point> pointsToSet = new ArrayList<>(points.size());

		for (Point point : points) {

			Assert.notNull(point, "Single Point in Polygon must not be null");
			pointsToSet.add(point);
		}

		this.points = Collections.unmodifiableList(pointsToSet);
	}

	/**
	 * Returns all {@link Point}s the {@link Polygon} is made of.
	 *
	 * @return
	 */
	public List<Point> getPoints() {
		return this.points;
	}

	@Override
	public Iterator<Point> iterator() {
		return this.points.iterator();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Polygon that)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(points, that.points);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(points);
	}

	@Override
	public String toString() {
		return String.format("Polygon: [%s]", StringUtils.collectionToCommaDelimitedString(points));
	}
}
