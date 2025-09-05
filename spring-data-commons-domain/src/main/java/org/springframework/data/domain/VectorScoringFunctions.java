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
 * Commonly used {@link ScoringFunction} implementations for vector-based similarity computations.
 * <p>
 * Provides a set of standard scoring strategies for comparing vectors in search or matching operations. Includes
 * options such as Euclidean distance, cosine similarity, and dot product.
 * <p>
 * These constants are intended for reuse across components requiring vector scoring semantics. Each scoring function
 * represents a mathematical approach to quantifying the similarity or distance between vectors in a multidimensional
 * space.
 * <p>
 * When selecting a scoring function, consider the specific requirements of your application domain:
 * <ul>
 * <li>For spatial distance measurements where magnitude matters, use {@link #EUCLIDEAN}.</li>
 * <li>For directional similarity irrespective of magnitude, use {@link #COSINE}.</li>
 * <li>For efficient high-dimensional calculations, use {@link #DOT_PRODUCT}.</li>
 * <li>For grid-based or axis-aligned problems, use {@link #TAXICAB}.</li>
 * <li>For binary vector or string comparisons, use {@link #HAMMING}.</li>
 * </ul>
 * The choice of scoring function can significantly impact the relevance of the results returned by a Vector Search
 * query. {@code ScoringFunction} and score values are typically subject to fine-tuning during the development to
 * achieve optimal performance and accuracy.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public enum VectorScoringFunctions implements ScoringFunction {

	/**
	 * Scoring based on the <a href="https://en.wikipedia.org/wiki/Euclidean_distance">Euclidean distance</a> between two
	 * vectors.
	 * <p>
	 * Computes the L2 norm, involving a square root operation. Typically more computationally expensive than
	 * {@link #COSINE} or {@link #DOT_PRODUCT}, but precise in spatial distance measurement.
	 */
	EUCLIDEAN,

	/**
	 * Scoring based on <a href="https://en.wikipedia.org/wiki/Cosine_distance">cosine similarity</a> between two vectors.
	 * <p>
	 * Measures the angle between vectors, independent of their magnitude. Involves a {@link #DOT_PRODUCT} and
	 * normalization, offering a balance between precision and performance.
	 */
	COSINE,

	/**
	 * Scoring based on the <a href="https://en.wikipedia.org/wiki/Dot_product">dot product</a> (also known as inner
	 * product) between two vectors.
	 * <p>
	 * Efficient to compute and particularly useful in high-dimensional vector spaces.
	 */
	DOT_PRODUCT,

	/**
	 * Scoring based on <a href="https://en.wikipedia.org/wiki/Taxicab_geometry">taxicab (Manhattan) distance</a>.
	 * <p>
	 * Computes the sum of absolute differences across dimensions. Useful in contexts where axis-aligned movement or L1
	 * norms are preferred.
	 */
	TAXICAB,

	/**
	 * Scoring based on the <a href="https://en.wikipedia.org/wiki/Hamming_distance">Hamming distance</a> between two
	 * vectors or strings.
	 * <p>
	 * Counts the number of differing positions. Suitable for binary (bitwise) vectors or fixed-length character
	 * sequences.
	 */
	HAMMING;

	@Override
	public String getName() {
		return name();
	}

}
