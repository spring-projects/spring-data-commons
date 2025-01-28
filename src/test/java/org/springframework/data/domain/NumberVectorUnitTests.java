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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NumberVector}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class NumberVectorUnitTests {

	Number[] values = new Number[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6f };
	Number[] floats = new Number[] { (float) 1.1d, (float) 2.2d, (float) 3.3d, (float) 4.4d, (float) 5.5, 6.6 };

	@Test // GH-3193
	void shouldErrorOnNullElements() {

		List<Long> source = new ArrayList<>(3);
		source.add(1L);
		source.add(null);
		source.add(3L);

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> NumberVector.copy(source));
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> NumberVector.copy(new Number[] { 1L, null, 3L }));
	}

	@Test // GH-3193
	void shouldAcceptEmptySource() {

		Vector vector = NumberVector.copy(List.of());

		assertThat(vector.size()).isEqualTo(0);
		assertThat(vector.getType()).isEqualTo(Number.class);

		vector = NumberVector.copy(new Number[] {});

		assertThat(vector.size()).isEqualTo(0);
		assertThat(vector.getType()).isEqualTo(Number.class);
	}

	@Test // GH-3193
	void shouldCreateVector() {

		Vector vector = Vector.of(Arrays.asList(values));

		assertThat(vector.size()).isEqualTo(6);
		assertThat(vector.getType()).isEqualTo(Number.class);
	}

	@Test // GH-3193
	void shouldCopyVectorValues() {

		Vector vector = Vector.of(Arrays.asList(values));

		assertThat(vector.getSource()).isNotSameAs(vector).isEqualTo(values);
	}

	@Test // GH-3193
	void shouldFigureOutCommonType() {

		assertThat(NumberVector.copy(List.of()).getType()).isEqualTo(Number.class);
		assertThat(NumberVector.copy(List.of(1)).getType()).isEqualTo(Integer.class);
		assertThat(NumberVector.copy(List.of(1L, 2L)).getType()).isEqualTo(Long.class);
		assertThat(NumberVector.copy(List.of(1F, 2F)).getType()).isEqualTo(Float.class);
		assertThat(NumberVector.copy(List.of(1D, 2D)).getType()).isEqualTo(Double.class);
		assertThat(NumberVector.copy(List.of(1D, 2F, 3F)).getType()).isEqualTo(Number.class);
	}

	@Test // GH-3193
	void shouldRenderToString() {

		Vector vector = Vector.of(Arrays.asList(values));

		assertThat(vector).hasToString("N[1.1, 2.2, 3.3, 4.4, 5.5, 6.6]");
	}

	@Test // GH-3193
	void shouldCompareVector() {

		Vector vector = Vector.of(Arrays.asList(values));

		assertThat(vector).isEqualTo(Vector.of(Arrays.asList(values)));
		assertThat(vector).hasSameHashCodeAs(Vector.of(Arrays.asList(values)));
	}

	@Test // GH-3193
	void sourceShouldReturnSource() { // this one is questionable

		Vector vector = new NumberVector(values);

		assertThat(vector.getSource()).isSameAs(values);
	}

	@Test // GH-3193
	void shouldCreateFloatArray() {

		Vector vector = Vector.of(Arrays.asList(values));

		float[] values = new float[this.floats.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = this.floats[i].floatValue();
		}

		assertThat(vector.toFloatArray()).isEqualTo(values).isNotSameAs(floats);
	}

	@Test // GH-3193
	void shouldCreateDoubleArray() {

		Vector vector = Vector.of(Arrays.asList(values));

		double[] values = new double[this.values.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = this.values[i].doubleValue();
		}

		assertThat(vector.toDoubleArray()).isEqualTo(values).isNotSameAs(values);
	}
}
