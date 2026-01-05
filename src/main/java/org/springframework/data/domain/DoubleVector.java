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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.ObjectUtils;

/**
 * {@link Vector} implementation based on {@code double} array.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.5
 */
class DoubleVector implements Vector {

	static final DoubleVector EMPTY = new DoubleVector(new double[0]) {

		@Override
		public float[] toFloatArray() {
			return FloatVector.EMPTY.v;
		}

		@Override
		public double[] toDoubleArray() {
			return this.v;
		}

	};

	final double[] v;

	DoubleVector(double[] v) {
		this.v = v;
	}

	/**
	 * Copy the given {@code double} array and wrap it within a Vector.
	 */
	static Vector copy(double[] v) {

		if (v.length == 0) {
			return EMPTY;
		}

		return new DoubleVector(Arrays.copyOf(v, v.length));
	}

	/**
	 * Copy the given numeric values and wrap within a Vector.
	 */
	static Vector copy(Collection<? extends Number> v) {

		if (v.isEmpty()) {
			return EMPTY;
		}

		double[] copy = new double[v.size()];
		int i = 0;
		for (Number number : v) {
			copy[i++] = number.doubleValue();
		}

		return new DoubleVector(copy);
	}

	@Override
	public Class<Double> getType() {
		return Double.TYPE;
	}

	@Override
	public Object getSource() {
		return v;
	}

	@Override
	public int size() {
		return v.length;
	}

	@Override
	public float[] toFloatArray() {

		float[] copy = new float[this.v.length];
		for (int i = 0; i < this.v.length; i++) {
			copy[i] = (float) this.v[i];
		}

		return copy;
	}

	@Override
	public double[] toDoubleArray() {
		return Arrays.copyOf(this.v, this.v.length);
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof DoubleVector that)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(v, that.v);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(v);
	}

	@Override
	public String toString() {
		return "D" + Arrays.toString(v);
	}
}
