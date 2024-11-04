/*
 * Copyright 2024 the original author or authors.
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
 * Unit tests for {@link NumberVector}.
 *
 * @author Mark Paluch
 */
class NumberVectorUnitTests {

	Number[] values = new Number[] { 1.1, 2.2, 3.3, 4.4, 5.5 };
	Number[] floats = new Number[] { (float) 1.1d, (float) 2.2d, (float) 3.3d, (float) 4.4d, (float) 5.5d };

	@Test // GH-3193
	void shouldCreateVector() {

		Vector vector = Vector.of(values);

		assertThat(vector.size()).isEqualTo(5);
		assertThat(vector.getType()).isEqualTo(Double.class);
	}

	@Test // GH-3193
	void shouldCopyVectorValues() {

		Vector vector = Vector.of(values);

		assertThat(vector.getSource()).isNotSameAs(vector).isEqualTo(values);
	}

	@Test // GH-3193
	void shouldRenderToString() {

		Vector vector = Vector.of(values);

		assertThat(vector).hasToString("N[1.1, 2.2, 3.3, 4.4, 5.5]");
	}

	@Test // GH-3193
	void shouldCompareVector() {

		Vector vector = Vector.of(values);

		assertThat(vector).isEqualTo(Vector.of(values));
		assertThat(vector).hasSameHashCodeAs(Vector.of(values));
	}

	@Test // GH-3193
	void sourceShouldReturnSource() {

		Vector vector = new NumberVector(values);

		assertThat(vector.getSource()).isSameAs(values);
	}

	@Test // GH-3193
	void shouldCreateFloatArray() {

		Vector vector = Vector.of(values);

		float[] values = new float[this.floats.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = this.floats[i].floatValue();
		}

		assertThat(vector.toFloatArray()).isEqualTo(values).isNotSameAs(floats);
	}

	@Test // GH-3193
	void shouldCreateDoubleArray() {

		Vector vector = Vector.of(values);

		double[] values = new double[this.values.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = this.values[i].doubleValue();
		}

		assertThat(vector.toDoubleArray()).isEqualTo(values).isNotSameAs(values);
	}
}
