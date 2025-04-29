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

/**
 * Strategy interface for scoring functions.
 * <p>
 * Implementations define how score (distance or similarity) between two vectors is computed, allowing control over
 * ranking behavior in search queries.
 * <p>
 * Provides commonly used scoring variants via static factory methods. See {@link VectorScoringFunctions} for the
 * concrete implementations.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see Score
 * @see Similarity
 */
public interface ScoringFunction {

	/**
	 * Returns the default {@code ScoringFunction} to be used when none is explicitly specified.
	 * <p>
	 * This is typically used to indicate the absence of a scoring definition.
	 *
	 * @return the default {@code ScoringFunction} instance.
	 */
	static ScoringFunction unspecified() {
		return UnspecifiedScoringFunction.INSTANCE;
	}

	/**
	 * Return the Euclidean distance scoring function.
	 * <p>
	 * Calculates the L2 norm (straight-line distance) between two vectors.
	 *
	 * @return the {@code ScoringFunction} based on Euclidean distance.
	 */
	static ScoringFunction euclidean() {
		return VectorScoringFunctions.EUCLIDEAN;
	}

	/**
	 * Return the cosine similarity scoring function.
	 * <p>
	 * Measures the cosine of the angle between two vectors, independent of magnitude.
	 *
	 * @return the {@code ScoringFunction} based on cosine similarity.
	 */
	static ScoringFunction cosine() {
		return VectorScoringFunctions.COSINE;
	}

	/**
	 * Return the dot product (also known as inner product) scoring function.
	 * <p>
	 * Computes the algebraic product of two vectors, considering both direction and magnitude.
	 *
	 * @return the {@code ScoringFunction} based on dot product.
	 */
	static ScoringFunction dotProduct() {
		return VectorScoringFunctions.DOT_PRODUCT;
	}

	/**
	 * Return the name of the scoring function.
	 * <p>
	 * Typically used for display or configuration purposes.
	 *
	 * @return the identifying name of this scoring function.
	 */
	String getName();

}
