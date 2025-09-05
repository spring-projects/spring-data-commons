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
 * Value object representing a normalized similarity score determined by a {@link ScoringFunction}.
 * <p>
 * Similarity values are constrained to the range {@code [0.0, 1.0]}, where {@code 0.0} denotes the least similarity and
 * {@code 1.0} the maximum similarity. This normalization allows for consistent comparison of similarity scores across
 * different scoring models and systems.
 * <p>
 * Primarily used in vector search and approximate nearest neighbor arrangements where results are ranked based on
 * normalized relevance. Vector searches typically return a collection of results ordered by their similarity to the
 * query vector.
 * <p>
 * This class is designed for use in information retrieval contexts, recommendation systems, and other applications
 * requiring normalized comparison of results.
 * <p>
 * A {@code Similarity} instance includes both the similarity {@code value} and information about the
 * {@link ScoringFunction} used to generate it, providing context for proper interpretation of the score.
 * <p>
 * Instances are immutable and support range-based comparisons, making them suitable for filtering and ranking
 * operations. The class extends {@link Score} to inherit common scoring functionality while adding similarity-specific
 * semantics.
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
	 * Creates a new {@link Similarity} from a plain {@code similarity} value using {@link ScoringFunction#unspecified()}.
	 *
	 * @param similarity the similarity value without a specific {@link ScoringFunction}, ranging between {@code 0} and
	 *          {@code 1}.
	 * @return the new {@link Similarity}.
	 */
	public static Similarity of(double similarity) {
		return of(similarity, ScoringFunction.unspecified());
	}

	/**
	 * Creates a new {@link Similarity} from a raw value and the associated {@link ScoringFunction}.
	 *
	 * @param similarity the similarity value in the {@code [0.0, 1.0]} range.
	 * @param function the scoring function that produced this similarity.
	 * @return a new {@link Similarity} instance.
	 * @throws IllegalArgumentException if the value is outside the allowed range.
	 */
	public static Similarity of(double similarity, ScoringFunction function) {

		Assert.isTrue(similarity >= 0.0 && similarity <= 1.0, "Similarity must be in [0,1] range.");

		return new Similarity(similarity, function);
	}

	/**
	 * Create a raw {@link Similarity} value without validation.
	 * <p>
	 * Intended for use when accepting similarity values from trusted sources such as search engines or databases.
	 *
	 * @param similarity the similarity value in the {@code [0.0, 1.0]} range.
	 * @param function the scoring function that produced this similarity.
	 * @return a new {@link Similarity} instance.
	 */
	public static Similarity raw(double similarity, ScoringFunction function) {
		return new Similarity(similarity, function);
	}

	/**
	 * Creates a {@link Range} between the given {@link Similarity}.
	 *
	 * @param min lower value.
	 * @param max upper value.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Similarity> between(Similarity min, Similarity max) {
		return Range.from(Range.Bound.inclusive(min)).to(Range.Bound.inclusive(max));
	}

	/**
	 * Creates a new {@link Range} by creating minimum and maximum {@link Similarity} from the given values
	 * {@link ScoringFunction#unspecified() without specifying} a specific scoring function.
	 *
	 * @param minValue lower value, ranging between {@code 0} and {@code 1}.
	 * @param maxValue upper value, ranging between {@code 0} and {@code 1}.
	 * @return the {@link Range} between the given values.
	 */
	public static Range<Similarity> between(double minValue, double maxValue) {
		return between(minValue, maxValue, ScoringFunction.unspecified());
	}

	/**
	 * Creates a {@link Range} of {@link Similarity} values using raw values and a specified scoring function.
	 *
	 * @param minValue the lower similarity value.
	 * @param maxValue the upper similarity value.
	 * @param function the scoring function to associate with the values.
	 * @return a {@link Range} of {@link Similarity} values.
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
