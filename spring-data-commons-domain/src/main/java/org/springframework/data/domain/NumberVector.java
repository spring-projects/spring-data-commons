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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@link Vector} implementation based on {@link Number} array.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 3.5
 */
class NumberVector implements Vector {

	static final NumberVector EMPTY = new NumberVector(new Number[0]) {

		@Override
		public float[] toFloatArray() {
			return FloatVector.EMPTY.v;
		}

		@Override
		public double[] toDoubleArray() {
			return DoubleVector.EMPTY.v;
		}

	};

	private final Number[] v;

	NumberVector(Number[] v) {

		Assert.noNullElements(v, "Vector [v] must not contain null elements");
		this.v = v;
	}

	/**
	 * Copy the given {@link Number} array and wrap it within a Vector.
	 */
	static Vector copy(Number[] v) {

		if (v.length == 0) {
			return EMPTY;
		}

		return new NumberVector(Arrays.copyOf(v, v.length));
	}

	/**
	 * Copy the given {@link Number} and wrap it within a Vector.
	 */
	static Vector copy(Collection<? extends Number> v) {

		if (v.isEmpty()) {
			return EMPTY;
		}

		return new NumberVector(v.toArray(Number[]::new));
	}

	@Override
	public Class<? extends Number> getType() {

		if (this.v.length == 0) {
			return Number.class;
		}

		Class<? extends Number> candidate = this.v[0].getClass();
		for (int i = 1; i < this.v.length; i++) {
			if (candidate != this.v[i].getClass()) {
				return Number.class;
			}
		}

		return candidate;
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
			copy[i] = this.v[i].floatValue();
		}

		return copy;
	}

	@Override
	public double[] toDoubleArray() {

		double[] copy = new double[this.v.length];
		for (int i = 0; i < this.v.length; i++) {
			copy[i] = this.v[i].doubleValue();
		}

		return copy;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof NumberVector that)) {
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
		return "N" + Arrays.toString(v);
	}
}
