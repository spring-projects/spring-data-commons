/*
 * Copyright 2015-2017 the original author or authors.
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
public class Range<T extends Comparable<T>> {

	private final static Range<?> UNBOUNDED = Range.of(Boundary.unbounded(), Boundary.UNBOUNDED);

	/**
	 * The lower bound of the range.
	 */
	private final Boundary<T> lowerBound;

	/**
	 * The upper bound of the range.
	 */
	private final Boundary<T> upperBound;

	/**
	 * Creates a new {@link Range} with the given lower and upper bound. Treats the given values as inclusive bounds. Use
	 * {@link #Range(Comparable, Comparable, boolean, boolean)} to configure different bound behavior.
	 * 
	 * @see Range#of(Boundary, Boundary)
	 * @param lowerBound can be {@literal null} in case upperBound is not {@literal null}.
	 * @param upperBound can be {@literal null} in case lowerBound is not {@literal null}.
	 */
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
	 * @deprecated since 2.0. Use {@link Range#of(Boundary, Boundary)} and {@link Boundary} factory methods:
	 *             {@link Boundary#inclusive(Comparable)},
	 *             {@link Boundary#exclusive(Comparable)}/{@link Boundary#unbounded()}.
	 */
	@Deprecated
	public Range(T lowerBound, T upperBound, boolean lowerInclusive, boolean upperInclusive) {

		this.lowerBound = lowerBound == null ? Boundary.unbounded()
				: lowerInclusive ? Boundary.inclusive(lowerBound) : Boundary.exclusive(lowerBound);

		this.upperBound = upperBound == null ? Boundary.unbounded()
				: upperInclusive ? Boundary.inclusive(upperBound) : Boundary.exclusive(upperBound);
	}

	private Range(Boundary<T> lowerBound, Boundary<T> upperBound) {

		Assert.notNull(lowerBound, "Lower boundary must not be null!");
		Assert.notNull(upperBound, "Upper boundary must not be null!");

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
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
	 * Create a {@link UpperRangeBuilder} for values greater than or equals {@code min} value.
	 * 
	 * @param min must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static <T extends Comparable<T>> UpperRangeBuilder<T> greaterThanOrEquals(T min) {
		return new UpperRangeBuilder<T>(Boundary.inclusive(min));
	}

	/**
	 * Create a {@link UpperRangeBuilder} for values greater than {@code min} value.
	 * 
	 * @param min must not be {@literal null}.
	 * @return
	 * @since 2.0
	 */
	public static <T extends Comparable<T>> UpperRangeBuilder<T> greaterThan(T min) {
		return new UpperRangeBuilder<T>(Boundary.exclusive(min));
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound.
	 * 
	 * @param lowerBound must not be {@literal null}.
	 * @param upperBound must not be {@literal null}.
	 * @since 2.0
	 */
	public static <T extends Comparable<T>> Range<T> of(Boundary<T> lowerBound, Boundary<T> upperBound) {
		return new Range<>(lowerBound, upperBound);
	}

	/**
	 * Creates a new {@link Integer} {@link Range} with the given lower and upper bound. Treats the given values as
	 * including bounds. Use {@link #of(Boundary, Boundary)} to configure different bound behavior.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @since 2.0
	 */
	public static Range<Integer> from(int lowerBoundInclusive, int upperBoundInclusive) {
		return of(Boundary.inclusive(lowerBoundInclusive), Boundary.inclusive(upperBoundInclusive));
	}

	/**
	 * Creates a new {@link Long} {@link Range} with the given lower and upper bound. Treats the given values as including
	 * bounds. Use {@link #of(Boundary, Boundary)} to configure different bound behavior.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @since 2.0
	 */
	public static Range<Long> from(long lowerBoundInclusive, long upperBoundInclusive) {
		return of(Boundary.inclusive(lowerBoundInclusive), Boundary.inclusive(upperBoundInclusive));
	}

	/**
	 * Creates a new {@link Float} {@link Range} with the given lower and upper bound. Treats the given values as
	 * including bounds. Use {@link #of(Boundary, Boundary)} to configure different bound behavior.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @since 2.0
	 */
	public static Range<Float> from(float lowerBoundInclusive, float upperBoundInclusive) {
		return of(Boundary.inclusive(lowerBoundInclusive), Boundary.inclusive(upperBoundInclusive));
	}

	/**
	 * Creates a new {@link Double} {@link Range} with the given lower and upper bound. Treats the given values as
	 * including bounds. Use {@link #of(Boundary, Boundary)} to configure different bound behavior.
	 * 
	 * @param lowerBound
	 * @param upperBound
	 * @since 2.0
	 */
	public static Range<Double> from(double lowerBoundInclusive, double upperBoundInclusive) {
		return of(Boundary.inclusive(lowerBoundInclusive), Boundary.inclusive(upperBoundInclusive));
	}

	/**
	 * @return
	 * @deprecated since 2.0, use {@link #getLowerBound()} and {@link Boundary#isInclusive()}.
	 */
	@Deprecated
	public boolean isLowerInclusive() {
		return lowerBound.isInclusive();
	}

	/**
	 * @return
	 * @deprecated since 2.0, use {@link #getUpperBound()} and {@link Boundary#isInclusive()}.
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

		String lower = this.lowerBound.getValue().map(Object::toString).map(it -> {

			if (this.lowerBound.isInclusive()) {
				return "[".concat(it);
			}
			return "(".concat(it);
		}).orElse("unbounded");

		String upper = this.upperBound.getValue().map(Object::toString).map(it -> {

			if (this.upperBound.isInclusive()) {
				return it.concat("]");
			}
			return it.concat(")");
		}).orElse("unbounded");

		return String.format("%s-%s", lower, upper);
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
	public static class Boundary<T extends Comparable<T>> {

		@SuppressWarnings({ "rawtypes",
				"unchecked" }) private static final Range.Boundary<?> UNBOUNDED = new Boundary(Optional.empty(), true);

		private final Optional<T> value;
		private final boolean inclusive;

		private Boundary(Optional<T> value, boolean inclusive) {

			this.value = value;
			this.inclusive = inclusive;
		}

		/**
		 * Creates an unbounded {@link Boundary}.
		 */
		@SuppressWarnings("unchecked")
		public static <T extends Comparable<T>> Boundary<T> unbounded() {
			return (Boundary<T>) UNBOUNDED;
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
		public static <T extends Comparable<T>> Boundary<T> inclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			return new Boundary<>(Optional.of(value), true);
		}

		/**
		 * Creates a boundary including {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Integer> inclusive(int value) {
			return inclusive((Integer) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Long> inclusive(long value) {
			return inclusive((Long) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Float> inclusive(float value) {
			return inclusive((Float) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Double> inclusive(double value) {
			return inclusive((Double) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static <T extends Comparable<T>> Boundary<T> exclusive(T value) {

			Assert.notNull(value, "Value must not be null!");
			return new Boundary<>(Optional.of(value), false);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Integer> exclusive(int value) {
			return exclusive((Integer) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Long> exclusive(long value) {
			return exclusive((Long) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Float> exclusive(float value) {
			return exclusive((Float) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 * 
		 * @param value must not be {@literal null}.
		 * @return
		 */
		public static Boundary<Double> exclusive(double value) {
			return exclusive((Double) value);
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
	public static class UpperRangeBuilder<T extends Comparable<T>> {

		private final Boundary<T> lower;

		UpperRangeBuilder(Boundary<T> lower) {
			this.lower = lower;
		}

		/**
		 * Create a {@link Range} for values less than or equals {@code max} value.
		 * 
		 * @param max must not be {@literal null}.
		 * @return
		 */
		public Range<T> andLessThanOrEquals(T max) {
			return new Range<>(lower, Boundary.inclusive(max));
		}

		/**
		 * Create a {@link Range} for values less than {@code max} value.
		 * 
		 * @param max must not be {@literal null}.
		 * @return
		 */
		public Range<T> andLessThan(T max) {
			return new Range<>(lower, Boundary.exclusive(max));
		}
	}
}
