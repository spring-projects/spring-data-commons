/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.domain;

import java.io.Serializable;

import org.springframework.util.ObjectUtils;

/**
 * Value object to represent search result scores determined by a {@link ScoringFunction}. Scores are used to rank
 * search results and typically, a higher score indicates a more relevant result.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public sealed class Score implements Serializable permits Similarity {

	private final double value;
	private final ScoringFunction function;

	Score(double value, ScoringFunction function) {
		this.value = value;
		this.function = function;
	}

	/**
	 * Creates a new {@link Score} from a plain {@code score} value using {@link ScoringFunction#UNSPECIFIED}.
	 *
	 * @param score the score value without a specific {@link ScoringFunction}.
	 * @return the new {@link Score}.
	 */
	public static Score of(double score) {
		return of(score, ScoringFunction.UNSPECIFIED);
	}

	/**
	 * Creates a new {@link Score} from a {@code score} value using the given {@link ScoringFunction}.
	 *
	 * @param score the score value.
	 * @param function the scoring function that has computed the {@code score}.
	 * @return the new {@link Score}.
	 */
	public static Score of(double score, ScoringFunction function) {
		return new Score(score, function);
	}

	/**
	 * Creates a {@link Range} between the given {@link Score}.
	 *
	 * @param min can be {@literal null}.
	 * @param max can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static Range<Score> between(Score min, Score max) {
		return Range.from(Range.Bound.inclusive(min)).to(Range.Bound.inclusive(max));
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Score} from the given values without
	 * {@link ScoringFunction#UNSPECIFIED specifying a scoring function}.
	 *
	 * @param minValue minimum value.
	 * @param maxValue maximum value.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Score> between(double minValue, double maxValue) {
		return between(minValue, maxValue, ScoringFunction.UNSPECIFIED);
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Score} from the given values.
	 *
	 * @param minValue minimum value.
	 * @param maxValue maximum value.
	 * @param function the scoring function to use.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Score> between(double minValue, double maxValue, ScoringFunction function) {
		return between(Score.of(minValue, function), Score.of(maxValue, function));
	}

	public double getValue() {
		return value;
	}

	public ScoringFunction getFunction() {
		return function;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Score other)) {
			return false;
		}
		if (value != other.value) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(function, other.function);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(value, function);
	}

	@Override
	public String toString() {
		return function instanceof UnspecifiedScoringFunction ? Double.toString(value)
				: "%s (%s)".formatted(Double.toString(value), function.getName());
	}

}
