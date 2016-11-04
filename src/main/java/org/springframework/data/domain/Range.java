/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.domain;

import lombok.Value;

import org.springframework.util.Assert;

/**
 * Simple value object to work with ranges.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
@Value
public class Range<T extends Comparable<T>> {

	/**
	 * The lower bound of the range.
	 */
	private final T lowerBound;

	/**
	 * The upper bound of the range.
	 */
	private final T upperBound;

	/**
	 * Whether the lower bound is considered inclusive.
	 */
	private final boolean lowerInclusive;

	/**
	 * Whether the lower bound is considered inclusive.
	 */
	private final boolean upperInclusive;

	/**
	 * Creates a new {@link Range} with the given lower and upper bound. Treats the given values as inclusive bounds. Use
	 * {@link #Range(Comparable, Comparable, boolean, boolean)} to configure different bound behavior.
	 * 
	 * @see #Range(Comparable, Comparable, boolean, boolean)
	 * @param lowerBound can be {@literal null} in case upperBound is not {@literal null}.
	 * @param upperBound can be {@literal null} in case lowerBound is not {@literal null}.
	 */
	public Range(T lowerBound, T upperBound) {
		this(lowerBound, upperBound, true, true);
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound as well as the given inclusive/exclusive
	 * semantics.
	 * 
	 * @param lowerBound can be {@literal null}.
	 * @param upperBound can be {@literal null}.
	 * @param lowerInclusive
	 * @param upperInclusive
	 */
	public Range(T lowerBound, T upperBound, boolean lowerInclusive, boolean upperInclusive) {

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.lowerInclusive = lowerInclusive;
		this.upperInclusive = upperInclusive;
	}

	/**
	 * Returns whether the {@link Range} contains the given value.
	 * 
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public boolean contains(T value) {

		Assert.notNull(value, "Reference value must not be null!");

		boolean greaterThanLowerBound = lowerBound == null ? true
				: lowerInclusive ? lowerBound.compareTo(value) <= 0 : lowerBound.compareTo(value) < 0;
		boolean lessThanUpperBound = upperBound == null ? true
				: upperInclusive ? upperBound.compareTo(value) >= 0 : upperBound.compareTo(value) > 0;

		return greaterThanLowerBound && lessThanUpperBound;
	}
}
