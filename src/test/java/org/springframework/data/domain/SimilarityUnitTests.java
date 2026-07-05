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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Similarity}.
 *
 * @author Mark Paluch
 */
class SimilarityUnitTests {

	@Test // GH-3285
	void shouldBeBounded() {

		assertThatIllegalArgumentException().isThrownBy(() -> Similarity.of(-1));
		assertThatIllegalArgumentException().isThrownBy(() -> Similarity.of(1.01));
	}

	@Test // GH-3285
	void shouldConstructRawSimilarity() {

		Similarity similarity = Similarity.raw(2, ScoringFunction.unspecified());

		assertThat(similarity.getValue()).isEqualTo(2);
	}

	@Test // GH-3285
	void shouldConstructGenericSimilarity() {

		Similarity similarity = Similarity.of(1);

		assertThat(similarity).isEqualTo(Similarity.of(1)).isNotEqualTo(Score.of(1)).isNotEqualTo(Similarity.of(0.5));
		assertThat(similarity).hasToString("1.0");
		assertThat(similarity.getFunction()).isEqualTo(ScoringFunction.unspecified());
	}

	@Test // GH-3285
	void shouldConstructMeteredSimilarity() {

		Similarity similarity = Similarity.of(1, VectorScoringFunctions.COSINE);

		assertThat(similarity).isEqualTo(Similarity.of(1, VectorScoringFunctions.COSINE))
				.isNotEqualTo(Score.of(1, VectorScoringFunctions.COSINE)).isNotEqualTo(Similarity.of(1));
		assertThat(similarity).hasToString("1.0 (COSINE)");
		assertThat(similarity.getFunction()).isEqualTo(VectorScoringFunctions.COSINE);
	}

	@Test // GH-3285
	void shouldConstructRange() {

		Range<Similarity> range = Similarity.between(0.5, 1);

		assertThat(range.getLowerBound().getValue()).contains(Similarity.of(0.5));
		assertThat(range.getLowerBound().isInclusive()).isTrue();

		assertThat(range.getUpperBound().getValue()).contains(Similarity.of(1));
		assertThat(range.getUpperBound().isInclusive()).isTrue();
	}

	@Test // GH-3285
	void shouldConstructRangeWithFunction() {

		Range<Similarity> range = Similarity.between(0.5, 1, VectorScoringFunctions.COSINE);

		assertThat(range.getLowerBound().getValue()).contains(Similarity.of(0.5, VectorScoringFunctions.COSINE));
		assertThat(range.getLowerBound().isInclusive()).isTrue();

		assertThat(range.getUpperBound().getValue()).contains(Similarity.of(1, VectorScoringFunctions.COSINE));
		assertThat(range.getUpperBound().isInclusive()).isTrue();
	}

}
