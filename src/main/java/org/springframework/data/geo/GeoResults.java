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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to capture {@link GeoResult}s as well as the average distance they have.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class GeoResults<T> implements Iterable<GeoResult<T>>, Serializable {

	private static final long serialVersionUID = 8347363491300219485L;

	private final List<? extends GeoResult<T>> results;
	private final Distance averageDistance;

	/**
	 * Creates a new {@link GeoResults} instance manually calculating the average distance from the distance values of the
	 * given {@link GeoResult}s.
	 * 
	 * @param results must not be {@literal null}.
	 */
	public GeoResults(List<? extends GeoResult<T>> results) {
		this(results, (Metric) null);
	}

	/**
	 * Creates a new {@link GeoResults} instance manually calculating the average distance in the given {@link Metric}
	 * from the distance values of the given {@link GeoResult}s.
	 * 
	 * @param results must not be {@literal null}.
	 * @param metric must not be {@literal null}.
	 */
	public GeoResults(List<? extends GeoResult<T>> results, Metric metric) {
		this(results, calculateAverageDistance(results, metric));
	}

	/**
	 * Creates a new {@link GeoResults} instance from the given {@link GeoResult}s and average distance.
	 * 
	 * @param results must not be {@literal null}.
	 * @param averageDistance can be {@literal null}.
	 */
	@PersistenceConstructor
	public GeoResults(List<? extends GeoResult<T>> results, Distance averageDistance) {

		Assert.notNull(results);

		this.results = results;
		this.averageDistance = averageDistance;
	}

	/**
	 * Returns the average distance of all {@link GeoResult}s in this list.
	 * 
	 * @return the averageDistance
	 */
	public Distance getAverageDistance() {
		return averageDistance;
	}

	/**
	 * Returns the actual content of the {@link GeoResults}.
	 * 
	 * @return
	 */
	public List<GeoResult<T>> getContent() {
		return Collections.unmodifiableList(results);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@SuppressWarnings("unchecked")
	public Iterator<GeoResult<T>> iterator() {
		return (Iterator<GeoResult<T>>) results.iterator();
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

		if (!(obj instanceof GeoResults)) {
			return false;
		}

		GeoResults<?> that = (GeoResults<?>) obj;

		return this.results.equals(that.results) && this.averageDistance.equals(that.averageDistance);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * results.hashCode();
		result += 31 * averageDistance.hashCode();

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("GeoResults: [averageDistance: %s, results: %s]", averageDistance.toString(),
				StringUtils.collectionToCommaDelimitedString(results));
	}

	private static Distance calculateAverageDistance(List<? extends GeoResult<?>> results, Metric metric) {

		if (results.isEmpty()) {
			return new Distance(0, metric);
		}

		double averageDistance = 0;

		for (GeoResult<?> result : results) {
			averageDistance += result.getDistance().getValue();
		}

		return new Distance(averageDistance / results.size(), metric);
	}
}
