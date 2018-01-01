/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.domain;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Optional;

import org.springframework.util.Assert;

/**
 * Simple value object to work with ranges and boundaries.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Range<T extends Comparable<T>> {

	private final static Range<?> UNBOUNDED = Range.of(Bound.unbounded(), Bound.UNBOUNDED);

	/**
	 * The lower bound of the range.
	 */
	private final @NonNull Bound<T> lowerBound;

	/**
	 * The upper bound of the range.
	 */
	private final @NonNull Bound<T> upperBound;

	/**
	 * Creates a new {@link Range} with the given lower and upper bound. Treats the given values as inclusive bounds. Use
	 * {@link #Range(Comparable, Comparable, boolean, boolean)} to configure different bound behavior.
	 *
	 * @see Range#of(Bound, Bound)
	 * @param lowerBound can be {@literal null} in case upperBound is not {@literal null}.
	 * @param upperBound can be {@literal null} in case lowerBound is not {@literal null}.
	 * @deprecated since 2.0 in favor of {@link Range#of(Bound, Bound)}.
	 */
	@Deprecated
	public Range(T lowerBound, T upperBound) {
		this(lowerBound, upperBound, true, true);
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound as well as the given inclusive/exclusive
	 * semantics.
	 *
	 * @param lowerBound can be {@literal null}.
	 * @param upperBound can be {@literal null}.
	 * @param lowerInclusive
	 * @param upperInclusive
	 * @deprecated since 2.0. Use {@link Range#of(Bound, Bound)} and {@link Bound} factory methods:
	 *             {@link Bound#inclusive(Comparable)}, {@link Bound#exclusive(Comparable)}/{@link Bound#unbounded()}.
	 */
	@Deprecated
	public Range(T lowerBound, T upperBound, boolean lowerInclusive, boolean upperInclusive) {

		this.lowerBound = lowerBound == null ? Bound.unbounded()
				: lowerInclusive ? Bound.inclusive(lowerBound) : Bound.exclusive(lowerBound);

		this.upperBound = upperBound == null ? Bound.unbounded()
				: upperInclusive ? Bound.inclusive(upperBound) : Bound.exclusive(upperBound);
	}

	/**
	 * Returns an unbounded {@link Range}.
	 *
	 * @return
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> Range<T> unbounded() {
		return (Range<T>) UNBOUNDED;
	}

	/**
	 * Create a {@link RangeBuilder} given the lower {@link Bound}.
	 *
	 * @param lower must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static <T extends Comparable<T>> RangeBuilder<T> from(Bound<T> lower) {

		Assert.notNull(lower, "Lower bound must not be null!");
		return new RangeBuilder<>(lower);
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound.
	 *
	 * @param lowerBound must not be {@literal null}.
	 * @param upperBound must not be {@literal null}.
	 * @since 2.0
	 */
	public static <T extends Comparable<T>> Range<T> of(Bound<T> lowerBound, Bound<T> upperBound) {
		return new Range<>(lowerBound, upperBound);
	}

	/**
	 * @return
	 * @deprecated since 2.0, use {@link #getLowerBound()} and {@link Bound#isInclusive()}.
	 */
	@Deprecated
	public boolean isLowerInclusive() {
		return lowerBound.isInclusive();
	}

	/**
	 * @return
	 * @deprecated since 2.0, use {@link #getUpperBound()} and {@link Bound#isInclusive()}.
	 */
	@Deprecated
	public boolean isUpperInclusive() {
		return upperBound.isInclusive();
	}

	/**
	 * Returns whether the {@link Range} contains the given value.
	 *
	 * @param value must not be {@literal null}.
	 * @return
	 */
	public boolean contains(T value) {

		Assert.notNull(value, "Reference value must not be null!");

		boolean greaterThanLowerBound = lowerBound.getValue() //
				.map(it -> lowerBound.isInclusive() ? it.compareTo(value) <= 0 : it.compareTo(value) < 0) //
				.orElse(true);

		boolean lessThanUpperBound = upperBound.getValue() //
				.map(it -> upperBound.isInclusive() ? it.compareTo(value) >= 0 : it.compareTo(value) > 0) //
				.orElse(true);

		return greaterThanLowerBound && lessThanUpperBound;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s-%s", lowerBound.toPrefixString(), upperBound.toSuffixString());
	}

	/**
	 * Value object representing a boundary. A boundary can either be {@link #unbounded() unbounded},
	 * {@link #inclusive(Comparable) including its value} or {@link #exclusive(Comparable) its value}.
	 *
	 * @author Mark Paluch
	 * @since 2.0
	 * @soundtrack Mohamed Ragab - Excelsior Sessions (March 2017)
	 */
	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public static class Bound<T extends Comparable<T>> {

		@SuppressWarnings({ "rawtypes", "unchecked" }) //
		private static final Bound<?> UNBOUNDED = new Bound(Optional.empty(), true);

		private final Optional<T> value;
		private final boolean inclusive;

		/**
		 * Creates an unbounded {@link Bound}.
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Comparable<T>> Bound<T> unbounded() {
			return (Bound<T>) UNBOUNDED;
		}

		/**
		 * Returns whether this boundary is bounded.
		 *
		 * @return
		 */
		public boolean isBounded() {
			return value.isPresent();
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static <T extends Comparable<T>> Bound<T> inclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			return new Bound<>(Optional.of(value), true);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Integer> inclusive(int value) {
			return inclusive((Integer) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Long> inclusive(long value) {
			return inclusive((Long) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Float> inclusive(float value) {
			return inclusive((Float) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Double> inclusive(double value) {
			return inclusive((Double) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static <T extends Comparable<T>> Bound<T> exclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			return new Bound<>(Optional.of(value), false);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Integer> exclusive(int value) {
			return exclusive((Integer) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Long> exclusive(long value) {
			return exclusive((Long) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Float> exclusive(float value) {
			return exclusive((Float) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Bound<Double> exclusive(double value) {
			return exclusive((Double) value);
		}

		String toPrefixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? "[".concat(it) : "(".concat(it)) //
					.orElse("unbounded");
		}

		String toSuffixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? it.concat("]") : it.concat(")")) //
					.orElse("unbounded");
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return value.map(Object::toString).orElse("unbounded");
		}

	}

	/**
	 * Builder for {@link Range} allowing to specify the upper boundary.
	 *
	 * @author Mark Paluch
	 * @since 2.0
	 * @soundtrack Aly and Fila - Future Sound Of Egypt 493
	 */
	public static class RangeBuilder<T extends Comparable<T>> {

		private final Bound<T> lower;

		RangeBuilder(Bound<T> lower) {
			this.lower = lower;
		}

		/**
		 * Create a {@link Range} given the upper {@link Bound}.
		 *
		 * @param upper must not be {@literal null}.
		 * @return
		 */
		public Range<T> to(Bound<T> upper) {

			Assert.notNull(upper, "Upper bound must not be null!");
			return new Range<>(lower, upper);
		}
	}
}
