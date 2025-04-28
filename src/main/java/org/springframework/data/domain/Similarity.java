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

import org.springframework.util.Assert;

/**
 * Value object to represent a similarity value determined by a {@link ScoringFunction}. Similarity is expressed through
 * a numerical value ranging between {@code 0} and {@code 1} where zero represents the lowest similarity and one the
 * highest similarity.
 * <p>
 * Similarity assumes normalized values and is typically used in vector search scenarios.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see Score
 */
public final class Similarity extends Score {

	private Similarity(double value, ScoringFunction function) {
		super(value, function);
	}

	/**
	 * Creates a new {@link Similarity} from a plain {@code similarity} value using {@link ScoringFunction#UNSPECIFIED}.
	 *
	 * @param similarity the similarity value without a specific {@link ScoringFunction}, ranging between {@code 0} and
	 *          {@code 1}.
	 * @return the new {@link Similarity}.
	 */
	public static Similarity of(double similarity) {
		return of(similarity, ScoringFunction.UNSPECIFIED);
	}

	/**
	 * Creates a new {@link Similarity} from a {@code similarity} value using the given {@link ScoringFunction}.
	 *
	 * @param similarity the similarity value, ranging between {@code 0} and {@code 1}.
	 * @param function the scoring function that has computed the {@code similarity}.
	 * @return the new {@link Similarity}.
	 */
	public static Similarity of(double similarity, ScoringFunction function) {

		Assert.isTrue(similarity >= 0.0 && similarity <= 1.0, "Similarity must be in [0,1] range.");

		return new Similarity(similarity, function);
	}

	/**
	 * Creates a new raw {@link Similarity} from a {@code similarity} value using the given {@link ScoringFunction}.
	 * Typically, this method is used when accepting external similarity values coming from a database search result.
	 *
	 * @param similarity the similarity value, ranging between {@code 0} and {@code 1}.
	 * @param function the scoring function that has computed the {@code similarity}.
	 * @return the new {@link Similarity}.
	 */
	public static Similarity raw(double similarity, ScoringFunction function) {
		return new Similarity(similarity, function);
	}

	/**
	 * Creates a {@link Range} between the given {@link Similarity}.
	 *
	 * @param min can be {@literal null}.
	 * @param max can be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static Range<Similarity> between(Similarity min, Similarity max) {
		return Range.from(Range.Bound.inclusive(min)).to(Range.Bound.inclusive(max));
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Similarity} from the given values without
	 * {@link ScoringFunction#UNSPECIFIED specifying a scoring function}.
	 *
	 * @param minValue minimum value, ranging between {@code 0} and {@code 1}.
	 * @param maxValue maximum value, ranging between {@code 0} and {@code 1}.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Similarity> between(double minValue, double maxValue) {
		return between(minValue, maxValue, ScoringFunction.UNSPECIFIED);
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Similarity} from the given values.
	 *
	 * @param minValue minimum value, ranging between {@code 0} and {@code 1}.
	 * @param maxValue maximum value, ranging between {@code 0} and {@code 1}.
	 * @param function the scoring function to use.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Similarity> between(double minValue, double maxValue, ScoringFunction function) {
		return between(Similarity.of(minValue, function), Similarity.of(maxValue, function));
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Similarity other)) {
			return false;
		}
		return super.equals(other);
	}

}
