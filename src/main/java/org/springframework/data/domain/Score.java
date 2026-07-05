/*
 * Copyright 2025-present the original author or authors.
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
 * Value object representing a search result score computed via a {@link ScoringFunction}.
 * <p>
 * Encapsulates the numeric score and the scoring function used to derive it. Scores are primarily used to rank search
 * results. Depending on the used {@link ScoringFunction} higher scores can indicate either a higher distance or a
 * higher similarity. Use the {@link Similarity} class to indicate usage of a normalized score across representing
 * effectively the similarity.
 * <p>
 * Instances of this class are immutable and suitable for use in comparison, sorting, and range operations.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see Similarity
 */
public sealed class Score implements Serializable permits Similarity {

	private final double value;
	private final ScoringFunction function;

	Score(double value, ScoringFunction function) {
		this.value = value;
		this.function = function;
	}

	/**
	 * Creates a new {@link Score} from a plain {@code score} value using {@link ScoringFunction#unspecified()}.
	 *
	 * @param score the score value without a specific {@link ScoringFunction}.
	 * @return the new {@link Score}.
	 */
	public static Score of(double score) {
		return of(score, ScoringFunction.unspecified());
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
	 * Creates a {@link Range} from the given minimum and maximum {@code Score} values.
	 *
	 * @param min the lower score value, must not be {@literal null}.
	 * @param max the upper score value, must not be {@literal null}.
	 * @return a {@link Range} over {@link Score} bounds.
	 */
	public static Range<Score> between(Score min, Score max) {
		return Range.from(Range.Bound.inclusive(min)).to(Range.Bound.inclusive(max));
	}

	/**
	 * Returns the raw numeric value of the score.
	 *
	 * @return the score value.
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Returns the {@link ScoringFunction} that was used to compute this score.
	 *
	 * @return the associated scoring function.
	 */
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
