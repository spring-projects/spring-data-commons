/*
 * Copyright 2015-2025 the original author or authors.
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

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Simple value object to work with ranges and boundaries.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.10
 */
public final class Range<T> {

	private static final Range<?> UNBOUNDED = Range.of(Bound.unbounded(), Bound.unbounded());

	/**
	 * The lower bound of the range.
	 */
	private final Bound<T> lowerBound;

	/**
	 * The upper bound of the range.
	 */
	private final Bound<T> upperBound;

	private Range(Bound<T> lowerBound, Bound<T> upperBound) {

		Assert.notNull(lowerBound, "Lower bound must not be null");
		Assert.notNull(upperBound, "Upper bound must not be null");

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	/**
	 * Returns an unbounded {@link Range}.
	 *
	 * @return an unbounded {@link Range}.
	 * @since 2.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> Range<T> unbounded() {
		return (Range<T>) UNBOUNDED;
	}

	/**
	 * Creates a new {@link Range} with inclusive bounds for both values.
	 *
	 * @param <T> the type of the range.
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return a {@link Range} with the lower bound set inclusively and the upper bound inclusively.
	 * @since 2.2
	 */
	public static <T> Range<T> closed(T from, T to) {
		return new Range<>(Bound.inclusive(from), Bound.inclusive(to));
	}

	/**
	 * Creates a new {@link Range} with exclusive bounds for both values.
	 *
	 * @param <T> the type of the range.
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return a {@link Range} with the lower bound set exclusively and the upper bound exclusively.
	 * @since 2.2
	 */
	public static <T> Range<T> open(T from, T to) {
		return new Range<>(Bound.exclusive(from), Bound.exclusive(to));
	}

	/**
	 * Creates a new left-open {@link Range}, i.e. left exclusive, right inclusive.
	 *
	 * @param <T> the type of the range.
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return a {@link Range} with the lower bound set exclusively and the upper bound inclusively.
	 * @since 2.2
	 */
	public static <T> Range<T> leftOpen(T from, T to) {
		return new Range<>(Bound.exclusive(from), Bound.inclusive(to));
	}

	/**
	 * Creates a new right-open {@link Range}, i.e. left inclusive, right exclusive.
	 *
	 * @param <T> the type of the range.
	 * @param from must not be {@literal null}.
	 * @param to must not be {@literal null}.
	 * @return a {@link Range} with the lower bound set inclusively and the upper bound exclusively.
	 * @since 2.2
	 */
	public static <T> Range<T> rightOpen(T from, T to) {
		return new Range<>(Bound.inclusive(from), Bound.exclusive(to));
	}

	/**
	 * Creates a left-unbounded {@link Range} (the left bound set to {@link Bound#unbounded()}) with the given right
	 * bound.
	 *
	 * @param <T> the type of the range.
	 * @param to the right {@link Bound}, must not be {@literal null}.
	 * @return a {@link Range} with the upper bound set to the given value and the lower side unbounded.
	 * @since 2.2
	 */
	public static <T> Range<T> leftUnbounded(Bound<T> to) {
		return new Range<>(Bound.unbounded(), to);
	}

	/**
	 * Creates a right-unbounded {@link Range} (the right bound set to {@link Bound#unbounded()}) with the given left
	 * bound.
	 *
	 * @param <T> the type of the range.
	 * @param from the left {@link Bound}, must not be {@literal null}.
	 * @return a {@link Range} with the lower bound set to the given value and the upper side unbounded.
	 * @since 2.2
	 */
	public static <T> Range<T> rightUnbounded(Bound<T> from) {
		return new Range<>(from, Bound.unbounded());
	}

	/**
	 * Create a {@link RangeBuilder} given the lower {@link Bound}.
	 *
	 * @param lower must not be {@literal null}.
	 * @return a range builder to continue creating a {@link Range} from the lower bound.
	 * @since 2.0
	 */
	public static <T> RangeBuilder<T> from(Bound<T> lower) {

		Assert.notNull(lower, "Lower bound must not be null");
		return new RangeBuilder<>(lower);
	}

	/**
	 * Creates a new {@link Range} with the given lower and upper bound. Prefer {@link #from(Bound)} for a more builder
	 * style API.
	 *
	 * @param lowerBound must not be {@literal null}.
	 * @param upperBound must not be {@literal null}.
	 * @since 2.0
	 * @see #from(Bound)
	 */
	public static <T> Range<T> of(Bound<T> lowerBound, Bound<T> upperBound) {
		return new Range<>(lowerBound, upperBound);
	}

	/**
	 * Creates a new Range with the given value as sole member.
	 *
	 * @param <T> the type of the range.>
	 * @param value must not be {@literal null}.
	 * @return a range containing the given value.
	 * @see Range#closed(Object, Object)
	 */
	public static <T> Range<T> just(T value) {
		return Range.closed(value, value);
	}

	/**
	 * Returns whether the {@link Range} contains the given value.
	 *
	 * @param value must not be {@literal null}.
	 * @return {@literal true} if the range contains the value; {@literal false} otherwise.
	 */
	@SuppressWarnings({ "unchecked" })
	public boolean contains(Comparable<T> value) {

		return contains((T) value, (o1, o2) -> {

			Assert.isInstanceOf(Comparable.class, o1,
					"Range value must be an instance of Comparable to use contains(Comparable<T>)");
			return ((Comparable<T>) o1).compareTo(o2);
		});
	}

	/**
	 * Returns whether the {@link Range} contains the given value.
	 *
	 * @param value must not be {@literal null}.
	 * @return {@literal true} if the range contains the value; {@literal false} otherwise.
	 * @since 3.0
	 */
	public boolean contains(T value, Comparator<T> comparator) {

		Assert.notNull(value, "Reference value must not be null");

		boolean greaterThanLowerBound = lowerBound.getValue() //
				.map(it -> lowerBound.isInclusive() ? comparator.compare(it, value) <= 0 : comparator.compare(it, value) < 0) //
				.orElse(true);

		boolean lessThanUpperBound = upperBound.getValue() //
				.map(it -> upperBound.isInclusive() ? comparator.compare(it, value) >= 0 : comparator.compare(it, value) > 0) //
				.orElse(true);

		return greaterThanLowerBound && lessThanUpperBound;
	}

	/**
	 * Apply a mapping {@link Function} to the lower and upper boundary values.
	 *
	 * @param mapper must not be {@literal null}. If the mapper returns {@literal null}, then the corresponding boundary
	 *          value represents an {@link Bound#unbounded()} boundary.
	 * @return a new {@link Range} after applying the value to the mapper.
	 * @param <R> target type of the mapping function.
	 * @since 3.0
	 */
	public <R> Range<R> map(Function<? super T, ? extends R> mapper) {

		Assert.notNull(mapper, "Mapping function must not be null");

		return Range.of(lowerBound.map(mapper), upperBound.map(mapper));
	}

	@Override
	public String toString() {
		return String.format("%s-%s", lowerBound.toPrefixString(), upperBound.toSuffixString());
	}

	public Range.Bound<T> getLowerBound() {
		return this.lowerBound;
	}

	public Range.Bound<T> getUpperBound() {
		return this.upperBound;
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Range<?> range)) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(lowerBound, range.lowerBound)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(upperBound, range.upperBound);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(lowerBound, upperBound);
	}

	/**
	 * Value object representing a boundary. A boundary can either be {@link #unbounded() unbounded},
	 * {@link #inclusive(Object) including its value} or {@link #exclusive(Object) its value}.
	 *
	 * @author Mark Paluch
	 * @since 2.0
	 * @soundtrack Mohamed Ragab - Excelsior Sessions (March 2017)
	 */
	public static final class Bound<T> {

		private static final Bound<?> UNBOUNDED = new Bound<>(Optional.empty(), true);

		private final Optional<T> value;
		private final boolean inclusive;

		private Bound(Optional<T> value, boolean inclusive) {
			this.value = value;
			this.inclusive = inclusive;
		}

		/**
		 * Creates an unbounded {@link Bound}.
		 */
		@SuppressWarnings("unchecked")
		public static <T> Bound<T> unbounded() {
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
		 */
		public static <T> Bound<T> inclusive(T value) {

			Assert.notNull(value, "Value must not be null");
			return Bound.of(Optional.of(value), true);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Integer> inclusive(int value) {
			return inclusive((Integer) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Long> inclusive(long value) {
			return inclusive((Long) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Float> inclusive(float value) {
			return inclusive((Float) value);
		}

		/**
		 * Creates a boundary including {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Double> inclusive(double value) {
			return inclusive((Double) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static <T> Bound<T> exclusive(T value) {

			Assert.notNull(value, "Value must not be null");
			return Bound.of(Optional.of(value), false);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Integer> exclusive(int value) {
			return exclusive((Integer) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Long> exclusive(long value) {
			return exclusive((Long) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
		 */
		public static Bound<Float> exclusive(float value) {
			return exclusive((Float) value);
		}

		/**
		 * Creates a boundary excluding {@code value}.
		 *
		 * @param value must not be {@literal null}.
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

		public Optional<T> getValue() {
			return this.value;
		}

		public boolean isInclusive() {
			return this.inclusive;
		}

		/**
		 * Apply a mapping {@link Function} to the boundary value.
		 *
		 * @param mapper must not be {@literal null}. If the mapper returns {@literal null}, then the boundary value
		 *          corresponds with {@link Bound#unbounded()}.
		 * @return a new {@link Bound} after applying the value to the mapper.
		 * @param <R>
		 * @since 3.0
		 */
		public <R> Bound<R> map(Function<? super T, ? extends R> mapper) {

			Assert.notNull(mapper, "Mapping function must not be null");

			return Bound.of(value.map(mapper), inclusive);
		}

		private static <R> Bound<R> of(Optional<R> value, boolean inclusive) {

			if (value.isPresent()) {
				return new Bound<>(value, inclusive);
			}

			return unbounded();
		}

		@Override
		public boolean equals(@Nullable Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof Bound<?> bound)) {
				return false;
			}

			if (value.isEmpty() && bound.value.isEmpty()) {
				return true;
			}

			if (inclusive != bound.inclusive)
				return false;

			return ObjectUtils.nullSafeEquals(value, bound.value);
		}

		@Override
		public int hashCode() {

			if (value.isEmpty()) {
				return ObjectUtils.nullSafeHashCode(value);
			}

			int result = ObjectUtils.nullSafeHashCode(value);
			result = 31 * result + (inclusive ? 1 : 0);
			return result;
		}

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
	public static class RangeBuilder<T> {

		private final Bound<T> lower;

		RangeBuilder(Bound<T> lower) {
			this.lower = lower;
		}

		/**
		 * Create a {@link Range} given the upper {@link Bound}.
		 *
		 * @param upper must not be {@literal null}.
		 */
		public Range<T> to(Bound<T> upper) {

			Assert.notNull(upper, "Upper bound must not be null");
			return new Range<>(lower, upper);
		}
	}
}
