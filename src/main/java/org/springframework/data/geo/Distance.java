/*
 * Copyright 2010-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.lang.CheckReturnValue;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Value object to represent distances in a given metric.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @since 1.8
 */
public final class Distance implements Serializable, Comparable<Distance> {

	private static final @Serial long serialVersionUID = 2460886201934027744L;

	/**
	 * The distance value in the current {@link Metric}.
	 */
	private final double value;

	/**
	 * The {@link Metric} of the {@link Distance}.
	 */
	private final Metric metric;

	/**
	 * Creates a new {@link Distance} with a neutral metric. This means the provided value needs to be in normalized form.
	 *
	 * @param value distance value.
	 */
	public Distance(double value) {
		this(value, Metrics.NEUTRAL);
	}

	/**
	 * Creates a new {@link Distance} with the given {@link Metric}.
	 *
	 * @param value
	 * @param metric must not be {@literal null}.
	 */
	public Distance(double value, Metric metric) {

		Assert.notNull(metric, "Metric must not be null");

		this.value = value;
		this.metric = metric;
	}

	/**
	 * Creates a new {@link Distance} with a neutral metric. This means the provided value needs to be in normalized form.
	 *
	 * @param value distance value.
	 * @since 4.0
	 */
	public static Distance of(double value) {
		return new Distance(value);
	}

	/**
	 * Creates a new {@link Distance} with the given {@link Metric}.
	 *
	 * @param value distance value.
	 * @param metric must not be {@literal null}.
	 * @since 4.0
	 */
	public static Distance of(double value, Metric metric) {
		return new Distance(value, metric);
	}

	/**
	 * Creates a {@link Range} between the given {@link Distance}.
	 *
	 * @param min can be {@literal null}.
	 * @param max can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static Range<Distance> between(Distance min, Distance max) {
		return Range.from(Bound.inclusive(min)).to(Bound.inclusive(max));
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Distance} from the given values.
	 *
	 * @param minValue minimum value.
	 * @param minMetric can be {@literal null}.
	 * @param maxValue maximum value.
	 * @param maxMetric can be {@literal null}.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Distance> between(double minValue, Metric minMetric, double maxValue, Metric maxMetric) {
		return between(new Distance(minValue, minMetric), new Distance(maxValue, maxMetric));
	}

	/**
	 * Returns the normalized value regarding the underlying {@link Metric}.
	 *
	 * @return the normalized value.
	 */
	public double getNormalizedValue() {
		return value / metric.getMultiplier();
	}

	/**
	 * Returns a {@link String} representation of the unit the distance is in.
	 *
	 * @return the unit
	 * @see Metric#getAbbreviation()
	 */
	public String getUnit() {
		return metric.getAbbreviation();
	}

	/**
	 * Adds the given distance to the current one. The resulting {@link Distance} will be in the same metric as the
	 * current one.
	 *
	 * @param other must not be {@literal null}.
	 * @return sum of this and the other distance.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public Distance add(Distance other) {

		Assert.notNull(other, "Distance to add must not be null");

		double newNormalizedValue = getNormalizedValue() + other.getNormalizedValue();

		return new Distance(newNormalizedValue * metric.getMultiplier(), metric);
	}

	/**
	 * Adds the given {@link Distance} to the current one and forces the result to be in a given {@link Metric}.
	 *
	 * @param other must not be {@literal null}.
	 * @param metric must not be {@literal null}.
	 * @return sum of this and the other distance.
	 */
	@Contract("_, _ -> new")
	public Distance add(Distance other, Metric metric) {

		Assert.notNull(other, "Distance to must not be null");
		Assert.notNull(metric, "Result metric must not be null");

		double newLeft = getNormalizedValue() * metric.getMultiplier();
		double newRight = other.getNormalizedValue() * metric.getMultiplier();

		return new Distance(newLeft + newRight, metric);
	}

	/**
	 * Returns a new {@link Distance} in the given {@link Metric}. This means that the returned instance will have the
	 * same normalized value as the original instance.
	 *
	 * @param metric must not be {@literal null}.
	 * @return the converted {@link Distance}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public Distance in(Metric metric) {

		Assert.notNull(metric, "Metric must not be null");

		return this.metric.equals(metric) ? this : new Distance(getNormalizedValue() * metric.getMultiplier(), metric);
	}

	@Override
	public int compareTo(@Nullable Distance that) {

		if (that == null) {
			return 1;
		}

		double difference = this.getNormalizedValue() - that.getNormalizedValue();

		return difference == 0 ? 0 : difference > 0 ? 1 : -1;
	}

	public double getValue() {
		return this.value;
	}

	public Metric getMetric() {
		return this.metric;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Distance distance)) {
			return false;
		}

		if (value != distance.value) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(metric, distance.metric);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(value, metric);
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();
		builder.append(value);

		if (metric != Metrics.NEUTRAL) {
			builder.append(" ").append(metric.toString());
		}

		return builder.toString();
	}
}
