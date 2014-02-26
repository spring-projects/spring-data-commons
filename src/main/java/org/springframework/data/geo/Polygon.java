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
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * Simple value object to represent a {@link Polygon}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class Polygon implements Iterable<Point> {

	private final List<Point> points;

	/**
	 * Creates a new {@link Polygon} for the given Points.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param others
	 */
	public <P extends Point> Polygon(P x, P y, P z, P... others) {

		Assert.notNull(x);
		Assert.notNull(y);
		Assert.notNull(z);
		Assert.notNull(others);

		this.points = new ArrayList<Point>(3 + others.length);
		this.points.addAll(Arrays.asList(x, y, z));
		this.points.addAll(Arrays.asList(others));
	}

	/**
	 * Creates a new {@link Polygon} for the given Points.
	 * 
	 * @param points
	 */
	public <P extends Point> Polygon(List<P> points) {

		Assert.notNull(points);

		this.points = new ArrayList<Point>(points.size());

		for (Point point : points) {
			Assert.notNull(point);
			this.points.add(point);
		}
	}

	public List<Point> getPoints() {

		List<Point> result = new ArrayList<Point>();

		for (Point point : points) {
			result.add(point);
		}

		return result;
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

		if (obj == null || !(obj instanceof Polygon)) {
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
}
