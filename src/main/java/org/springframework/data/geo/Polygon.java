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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Simple value object to represent a {@link Polygon}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Polygon implements Iterable<Point>, Shape {

	private static final long serialVersionUID = -2705040068154648988L;

	private final List<Point> points;

	/**
	 * Creates a new {@link Polygon} for the given Points.
	 * 
	 * @param x must not be {@literal null}.
	 * @param y must not be {@literal null}.
	 * @param z must not be {@literal null}.
	 * @param others
	 */
	public Polygon(Point x, Point y, Point z, Point... others) {

		Assert.notNull(x, "X coordinate must not be null!");
		Assert.notNull(y, "Y coordinate must not be null!");
		Assert.notNull(z, "Z coordinate must not be null!");
		Assert.notNull(others);

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
	@PersistenceConstructor
	public Polygon(List<? extends Point> points) {

		Assert.notNull(points);

		List<Point> pointsToSet = new ArrayList<>(points.size());

		for (Point point : points) {

			Assert.notNull(point);
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

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Point> iterator() {
		return this.points.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Polygon)) {
			return false;
		}

		Polygon that = (Polygon) obj;

		return this.points.equals(that.points);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return points.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Polygon: [%s]", StringUtils.collectionToCommaDelimitedString(points));
	}
}
