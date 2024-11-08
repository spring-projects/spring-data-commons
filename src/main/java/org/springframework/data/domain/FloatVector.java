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

import java.util.Arrays;

import org.springframework.util.ObjectUtils;

/**
 * {@link Vector} implementation based on {@code float} array.
 *
 * @author Mark Paluch
 * @since 3.5
 */
class FloatVector implements Vector {

	private final float[] v;

	public FloatVector(float[] v) {
		this.v = v;
	}

	/**
	 * Copy the given {@code float} array and wrap it within a Vector.
	 */
	static Vector copy(float[] v) {

		float[] copy = new float[v.length];
		System.arraycopy(v, 0, copy, 0, copy.length);

		return new FloatVector(copy);
	}

	@Override
	public Class<Float> getType() {
		return Float.TYPE;
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
		System.arraycopy(this.v, 0, copy, 0, copy.length);

		return copy;
	}

	@Override
	public double[] toDoubleArray() {

		double[] copy = new double[this.v.length];
		for (int i = 0; i < this.v.length; i++) {
			copy[i] = this.v[i];
		}

		return copy;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}
		if (!(o instanceof FloatVector that)) {
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
		return "F" + Arrays.toString(v);
	}
}
