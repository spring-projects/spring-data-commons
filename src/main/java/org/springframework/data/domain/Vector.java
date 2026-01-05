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

import java.util.Collection;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A vector is a fixed-length array of non-null numeric values. Vectors are represent a point in a multidimensional
 * space that is commonly used in machine learning and statistics.
 * <p>
 * Vector properties do not map cleanly to an existing class in the standard JDK Collections hierarchy. Vectors when
 * used with embeddings (machine learning) represent an opaque point in the vector space that does not expose meaningful
 * properties nor guarantees computational values to the outside world.
 * <p>
 * Vectors should be treated as opaque values and should not be modified. They can be created from an array of numbers
 * (typically {@code double} or {@code float} values) and used by components that need to provide the vector for storage
 * or computation.
 *
 * @author Mark Paluch
 * @since 3.5
 */
public interface Vector {

	/**
	 * Creates a new {@link Vector} from the given float {@code values}. Vector values are duplicated to avoid capturing a
	 * mutable array instance and to prevent mutability.
	 *
	 * @param values float vector values.
	 * @return the {@link Vector} for the given vector values.
	 */
	static Vector of(float... values) {

		Assert.notNull(values, "float vector values must not be null");

		return FloatVector.copy(values);
	}

	/**
	 * Creates a new {@link Vector} from the given double {@code values}. Vector values are duplicated to avoid capturing
	 * a mutable array instance and to prevent mutability.
	 *
	 * @param values double vector values.
	 * @return the {@link Vector} for the given vector values.
	 */
	static Vector of(double... values) {

		Assert.notNull(values, "double vector values must not be null");

		return DoubleVector.copy(values);
	}

	/**
	 * Creates a new {@link Vector} from the given number {@code values}. Vector values are duplicated to avoid capturing
	 * a mutable collection instance and to prevent mutability.
	 *
	 * @param values number vector values.
	 * @return the {@link Vector} for the given vector values.
	 */
	static Vector of(Collection<? extends Number> values) {

		Assert.notNull(values, "Vector values must not be null");

		if (values.isEmpty()) {
			return NumberVector.EMPTY;
		}

		Class<?> cet = CollectionUtils.findCommonElementType(values);

		if (cet == Double.class) {
			return DoubleVector.copy(values);
		}

		if (cet == Float.class) {
			return FloatVector.copy(values);
		}

		return NumberVector.copy(values);
	}

	/**
	 * Creates a new unsafe {@link Vector} wrapper from the given {@code values}. Unsafe wrappers do not duplicate array
	 * values and are merely a view on the source array.
	 * <p>
	 * Supported source type
	 *
	 * @param values vector values.
	 * @return the {@link Vector} for the given vector values.
	 */
	static Vector unsafe(float[] values) {

		Assert.notNull(values, "float vector values must not be null");

		return new FloatVector(values);
	}

	/**
	 * Creates a new unsafe {@link Vector} wrapper from the given {@code values}. Unsafe wrappers do not duplicate array
	 * values and are merely a view on the source array.
	 * <p>
	 * Supported source type
	 *
	 * @param values vector values.
	 * @return the {@link Vector} for the given vector values.
	 */
	static Vector unsafe(double[] values) {

		Assert.notNull(values, "double vector values must not be null");

		return new DoubleVector(values);
	}

	/**
	 * Returns the type of the underlying vector source.
	 *
	 * @return the type of the underlying vector source.
	 */
	Class<? extends Number> getType();

	/**
	 * Returns the source array of the vector. The source array is not copied and should not be modified to avoid
	 * mutability issues. This method should be used for performance access.
	 *
	 * @return the source array of the vector.
	 */
	Object getSource();

	/**
	 * Returns the number of dimensions.
	 *
	 * @return the number of dimensions.
	 */
	int size();

	/**
	 * Convert the vector to a {@code float} array. The returned array is a copy of the {@link #getSource() source} array
	 * and can be modified safely.
	 * <p>
	 * Conversion to {@code float} can incorporate loss of precision or result in values with a slight offset due to data
	 * type conversion if the source is not a {@code float} array.
	 * <p>
	 * Note that Vectors using quantization or binary representations may not be convertible to a {@code float} array.
	 *
	 * @return a new {@code float} array representing the vector point.
	 */
	float[] toFloatArray();

	/**
	 * Convert the vector to a {@code double} array. The returned array is a copy of the {@link #getSource() source} array
	 * and can be modified safely.
	 * <p>
	 * Conversion to {@code double} can incorporate loss of precision or result in values with a slight offset due to data
	 * type conversion if the source is not a {@code double} array.
	 * <p>
	 * Note that Vectors using quantization or binary representations may not be convertible to a {@code double} array.
	 *
	 * @return a new {@code double} array representing the vector point.
	 */
	double[] toDoubleArray();

}
