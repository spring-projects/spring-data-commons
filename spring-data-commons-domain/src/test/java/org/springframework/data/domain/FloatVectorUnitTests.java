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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FloatVector}.
 *
 * @author Mark Paluch
 */
class FloatVectorUnitTests {

	float[] values = new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
	double[] doubles = new double[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };

	@Test // GH-3193
	void shouldCreateVector() {

		Vector vector = Vector.of(values);

		assertThat(vector.size()).isEqualTo(5);
		assertThat(vector.getType()).isEqualTo(Float.TYPE);
	}

	@Test // GH-3193
	void shouldCreateUnsafeVector() {

		Vector vector = Vector.unsafe(values);

		assertThat(vector.getSource()).isSameAs(values);
	}

	@Test // GH-3193
	void shouldCopyVectorValues() {

		Vector vector = Vector.of(values);

		assertThat(vector.getSource()).isNotSameAs(vector).isEqualTo(values);
	}

	@Test // GH-3193
	void shouldRenderToString() {

		Vector vector = Vector.of(values);

		assertThat(vector).hasToString("F[1.1, 2.2, 3.3, 4.4, 5.5]");
	}

	@Test // GH-3193
	void shouldCompareVector() {

		Vector vector = Vector.of(values);

		assertThat(vector).isEqualTo(Vector.of(values));
		assertThat(vector).hasSameHashCodeAs(Vector.of(values));
	}

	@Test // GH-3193
	void sourceShouldReturnSource() {

		Vector vector = new FloatVector(values);

		assertThat(vector.getSource()).isSameAs(values);
	}

	@Test // GH-3193
	void shouldCreateFloatArray() {

		Vector vector = Vector.of(values);

		assertThat(vector.toFloatArray()).isEqualTo(values).isNotSameAs(values);
	}

	@Test // GH-3193
	void shouldCreateDoubleArray() {

		Vector vector = Vector.of(values);

		assertThat(vector.toDoubleArray()).isEqualTo(doubles).isNotSameAs(doubles);
	}
}
