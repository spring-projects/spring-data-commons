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
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object to capture {@link GeoResult}s as well as the average distance they have.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public class GeoResults<T> implements Iterable<GeoResult<T>>, Serializable {

	private static final @Serial long serialVersionUID = 8347363491300219485L;

	private final List<? extends GeoResult<T>> results;
	private final Distance averageDistance;

	/**
	 * Creates a new {@link GeoResults} instance manually calculating the average distance from the distance values of the
	 * given {@link GeoResult}s.
	 *
	 * @param results must not be {@literal null}.
	 */
	public GeoResults(List<? extends GeoResult<T>> results) {
		this(results, Metrics.NEUTRAL);
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
	 * @param averageDistance must not be {@literal null}.
	 */
	@PersistenceCreator
	public GeoResults(List<? extends GeoResult<T>> results, Distance averageDistance) {

		Assert.notNull(results, "Results must not be null");
		Assert.notNull(averageDistance, "Average Distance must not be null");

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
	 * @return the actual content.
	 */
	public List<GeoResult<T>> getContent() {
		return Collections.unmodifiableList(results);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<GeoResult<T>> iterator() {
		return (Iterator<GeoResult<T>>) results.iterator();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof GeoResults<?> that)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(results, that.results)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(averageDistance, that.averageDistance);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(results, averageDistance);
	}

	@Override
	public String toString() {
		return results.isEmpty() ? "GeoResults [empty]"
				: String.format("GeoResults [averageDistance: %s, size: %s]", averageDistance, results.size());
	}

	private static Distance calculateAverageDistance(List<? extends GeoResult<?>> results, Metric metric) {

		Assert.notNull(results, "Results must not be null");
		Assert.notNull(metric, "Metric must not be null");

		if (results.isEmpty()) {
			return new Distance(0, metric);
		}

		double averageDistance = results.stream()//
				.mapToDouble(it -> it.getDistance().getValue())//
				.average().orElse(0);

		return new Distance(averageDistance, metric);
	}
}
