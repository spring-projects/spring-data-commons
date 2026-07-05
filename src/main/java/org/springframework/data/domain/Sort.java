/*
 * Copyright 2008-present the original author or authors.
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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.util.MethodInvocationRecorder;
import org.springframework.data.util.MethodInvocationRecorder.Recorded;
import org.springframework.data.util.Streamable;
import org.springframework.lang.CheckReturnValue;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Sort option for queries. You have to provide at least a list of properties to sort for that must not include
 * {@literal null} or empty strings. The direction defaults to {@link Sort#DEFAULT_DIRECTION}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @author Jan Kurella
 */
public class Sort implements Streamable<org.springframework.data.domain.Sort.Order>, Serializable {

	private static final @Serial long serialVersionUID = 5737186511678863905L;

	private static final Sort UNSORTED = Sort.by(new Order[0]);

	public static final Direction DEFAULT_DIRECTION = Direction.ASC;

	private final List<Order> orders;

	protected Sort(List<Order> orders) {
		this.orders = orders;
	}

	/**
	 * Creates a new {@link Sort} instance.
	 *
	 * @param direction defaults to {@link Sort#DEFAULT_DIRECTION} (for {@literal null} cases, too)
	 * @param properties must not be {@literal null} or contain {@literal null} or empty strings.
	 */
	private Sort(Direction direction, @Nullable List<String> properties) {

		if (properties == null || properties.isEmpty()) {
			throw new IllegalArgumentException("You have to provide at least one property to sort by");
		}

		this.orders = properties.stream() //
				.map(it -> new Order(direction, it)) //
				.collect(Collectors.toList());
	}

	/**
	 * Creates a new {@link Sort} for the given properties.
	 *
	 * @param properties must not be {@literal null}.
	 * @return {@link Sort} for the given properties.
	 */
	public static Sort by(String... properties) {

		Assert.notNull(properties, "Properties must not be null");

		return properties.length == 0 //
				? Sort.unsorted() //
				: new Sort(DEFAULT_DIRECTION, Arrays.asList(properties));
	}

	/**
	 * Creates a new {@link Sort} for the given properties.
	 *
	 * @param properties must not be {@literal null}.
	 * @return {@link Sort} for the given properties.
	 * @since 4.1
	 */
	@SafeVarargs
	public static <T> Sort by(TypedPropertyPath<T, ?>... properties) {

		Assert.notNull(properties, "Properties must not be null");

		return properties.length == 0 //
				? Sort.unsorted() //
				: new Sort(DEFAULT_DIRECTION,
						Arrays.stream(properties).map(PropertyPath::toDotPath)
						.collect(Collectors.toList()));
	}

	/**
	 * Creates a new {@link Sort} for the given {@link Order}s.
	 *
	 * @param orders must not be {@literal null}.
	 * @return {@link Sort} for the given {@link Order}s.
	 */
	public static Sort by(List<Order> orders) {

		Assert.notNull(orders, "Orders must not be null");

		return orders.isEmpty() ? Sort.unsorted() : new Sort(orders);
	}

	/**
	 * Creates a new {@link Sort} for the given {@link Order}s.
	 *
	 * @param orders must not be {@literal null}.
	 * @return {@link Sort} for the given {@link Order}s.
	 */
	public static Sort by(Order... orders) {

		Assert.notNull(orders, "Orders must not be null");

		return new Sort(Arrays.asList(orders));
	}

	/**
	 * Creates a new {@link Sort} for the given {@link Direction} and properties.
	 *
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @return {@link Sort} for the given {@link Direction} and properties.
	 * @since 4.1
	 */
	@SafeVarargs
	public static <T> Sort by(Direction direction, TypedPropertyPath<T, ?>... properties) {

		Assert.notNull(direction, "Direction must not be null");
		Assert.notNull(properties, "Properties must not be null");
		Assert.isTrue(properties.length > 0, "At least one property must be given");

		return by(Arrays.stream(properties).map(PropertyPath::toDotPath)
				.map(it -> new Order(direction, it)).toList());
	}

	/**
	 * Creates a new {@link Sort} for the given {@link Direction} and properties.
	 *
	 * @param direction must not be {@literal null}.
	 * @param properties must not be {@literal null}.
	 * @return {@link Sort} for the given {@link Direction} and properties.
	 */
	public static Sort by(Direction direction, String... properties) {

		Assert.notNull(direction, "Direction must not be null");
		Assert.notNull(properties, "Properties must not be null");
		Assert.isTrue(properties.length > 0, "At least one property must be given");

		return Sort.by(Arrays.stream(properties)//
				.map(it -> new Order(direction, it))//
				.collect(Collectors.toList()));
	}

	/**
	 * Creates a new {@link TypedSort} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@link TypedSort} for the given type.
	 * @since 2.2
	 * @deprecated since 4.1 in favor of {@link Sort#by(TypedPropertyPath[])}.
	 */
	@Deprecated(since = "4.1")
	public static <T> TypedSort<T> sort(Class<T> type) {
		return new TypedSort<>(type);
	}

	/**
	 * Returns a {@link Sort} instances representing no sorting setup at all.
	 *
	 * @return unsorted Sort instance.
	 */
	public static Sort unsorted() {
		return UNSORTED;
	}

	/**
	 * Returns a new {@link Sort} with the current setup but descending order direction.
	 *
	 * @return a new {@link Sort} with the current setup but descending order direction.
	 */
	@Contract("-> new")
	public Sort descending() {
		return withDirection(Direction.DESC);
	}

	/**
	 * Returns a new {@link Sort} with the current setup but ascending order direction.
	 *
	 * @return a new {@link Sort} with the current setup but ascending order direction.
	 */
	@Contract("-> new")
	public Sort ascending() {
		return withDirection(Direction.ASC);
	}

	/**
	 * @return {@literal true} if this Sort instance is sorted, {@literal false} otherwise.
	 */
	public boolean isSorted() {
		return !isEmpty();
	}

	@Override
	public boolean isEmpty() {
		return orders.isEmpty();
	}

	/**
	 * @return {@literal true} if this Sort instance is unsorted, {@literal false} otherwise.
	 */
	public boolean isUnsorted() {
		return !isSorted();
	}

	/**
	 * Returns a new {@link Sort} consisting of the {@link Order}s of the current {@link Sort} combined with the given
	 * ones.
	 *
	 * @param sort must not be {@literal null}.
	 * @return a new {@link Sort} consisting of the {@link Order}s of the current {@link Sort} combined with the given
	 *         ones.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	public Sort and(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		List<Order> these = new ArrayList<>(this.toList());

		for (Order order : sort) {
			these.add(order);
		}

		return Sort.by(these);
	}

	/**
	 * Returns a new {@link Sort} with reversed sort {@link Order}s turning effectively asccending into descending sort
	 * order and vice versa.
	 *
	 * @return a new {@link Sort} object with reversed sort orders applied.
	 * @since 3.1
	 */
	@Contract("-> new")
	public Sort reverse() {

		List<Order> reversed = doReverse();

		return Sort.by(reversed);
	}

	protected List<Order> doReverse() {

		List<Order> reversed = new ArrayList<>(orders.size());

		for (Order order : this) {
			reversed.add(order.reverse());
		}

		return reversed;
	}

	/**
	 * Returns the order registered for the given property.
	 *
	 * @param property name of the property that should be sorted.
	 * @return the sort {@link Order} or {@literal null} if the property is not sorted by.
	 */
	public @Nullable Order getOrderFor(String property) {

		for (Order order : this) {
			if (order.getProperty().equals(property)) {
				return order;
			}
		}

		return null;
	}

	@Override
	public Iterator<Order> iterator() {
		return this.orders.iterator();
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Sort that)) {
			return false;
		}

		return toList().equals(that.toList());
	}

	@Override
	public int hashCode() {
		return orders.hashCode();
	}

	@Override
	public String toString() {
		return isEmpty() ? "UNSORTED" : StringUtils.collectionToCommaDelimitedString(orders);
	}

	private Sort withDirection(Direction direction) {

		List<Order> result = new ArrayList<>(orders.size());

		for (Order order : this) {
			result.add(order.with(direction));
		}

		return Sort.by(result);
	}

	/**
	 * Enumeration for sort directions.
	 *
	 * @author Oliver Gierke
	 */
	public enum Direction {

		ASC, DESC;

		/**
		 * Returns whether the direction is ascending.
		 *
		 * @return {@literal true} if ascending, {@literal false} otherwise.
		 * @since 1.13
		 */
		public boolean isAscending() {
			return this.equals(ASC);
		}

		/**
		 * Returns whether the direction is descending.
		 *
		 * @return {@literal true} if descending, {@literal false} otherwise.
		 * @since 1.13
		 */
		public boolean isDescending() {
			return this.equals(DESC);
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} value.
		 *
		 * @param value the direction name.
		 * @return the {@link Direction} enum value for the given {@code value}.
		 * @throws IllegalArgumentException in case the given value cannot be parsed into an enum value.
		 */
		public static Direction fromString(String value) {

			try {
				return Direction.valueOf(value.toUpperCase(Locale.US));
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Invalid value '%s' for orders given; Has to be either 'desc' or 'asc' (case insensitive)", value), e);
			}
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} or {@code Optional.empty()} if it cannot be
		 * parsed into an enum value.
		 *
		 * @param value the direction name.
		 * @return Optional holding the {@link Direction} enum value or empty, if {@code value} cannot be parsed into
		 *         {@link Direction}.
		 */
		public static Optional<Direction> fromOptionalString(String value) {

			if (ObjectUtils.isEmpty(value)) {
				return Optional.empty();
			}

			try {
				return Optional.of(fromString(value));
			} catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}
	}

	/**
	 * Enumeration for null handling hints that can be used in {@link Order} expressions.
	 *
	 * @author Thomas Darimont
	 * @since 1.8
	 */
	public enum NullHandling {

		/**
		 * Lets the data store decide what to do with nulls.
		 */
		NATIVE,

		/**
		 * A hint to the used data store to order entries with null values before non-null entries.
		 */
		NULLS_FIRST,

		/**
		 * A hint to the used data store to order entries with null values after non-null entries.
		 */
		NULLS_LAST
	}

	/**
	 * PropertyPath implements the pairing of an {@link Direction} and a property. It is used to provide input for
	 * {@link Sort}
	 *
	 * @author Oliver Gierke
	 * @author Kevin Raymond
	 * @author Jens Schauder
	 */
	public static class Order implements Serializable {

		private static final @Serial long serialVersionUID = 1522511010900108987L;

		private static final boolean DEFAULT_IGNORE_CASE = false;
		private static final NullHandling DEFAULT_NULL_HANDLING = NullHandling.NATIVE;

		private final Direction direction;
		private final String property;
		private final boolean ignoreCase;
		private final NullHandling nullHandling;

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 *
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
		 * @param property must not be {@literal null} or empty.
		 */
		public Order(@Nullable Direction direction, String property) {
			this(direction, property, DEFAULT_IGNORE_CASE, DEFAULT_NULL_HANDLING);
		}

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 *
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
		 * @param property must not be {@literal null} or empty.
		 * @param nullHandlingHint must not be {@literal null}.
		 */
		public Order(@Nullable Direction direction, String property, NullHandling nullHandlingHint) {
			this(direction, property, DEFAULT_IGNORE_CASE, nullHandlingHint);
		}

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 *
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
		 * @param property must not be {@literal null} or empty.
		 * @param ignoreCase true if sorting should be case-insensitive. false if sorting should be case-sensitive.
		 * @param nullHandling must not be {@literal null}.
		 * @since 1.7
		 */
		public Order(@Nullable Direction direction, String property, boolean ignoreCase, NullHandling nullHandling) {

			if (!StringUtils.hasText(property)) {
				throw new IllegalArgumentException("Property must not be null or empty");
			}

			this.direction = direction == null ? DEFAULT_DIRECTION : direction;
			this.property = property;
			this.ignoreCase = ignoreCase;
			this.nullHandling = nullHandling;
		}

		/**
		 * Creates a new {@link Order} instance. Takes a property path. Direction defaults to
		 * {@link Sort#DEFAULT_DIRECTION}.
		 *
		 * @param propertyPath must not be {@literal null} or empty.
		 * @since 4.1
		 */
		public static <T, P> Order by(TypedPropertyPath<T, P> propertyPath) {
			return by(propertyPath.toDotPath());
		}

		/**
		 * Creates a new {@link Order} instance. Takes a single property. Direction defaults to
		 * {@link Sort#DEFAULT_DIRECTION}.
		 *
		 * @param property must not be {@literal null} or empty.
		 * @since 2.0
		 */
		public static Order by(String property) {
			return new Order(DEFAULT_DIRECTION, property);
		}

		/**
		 * Creates a new {@link Order} instance. Takes a property path. Direction is {@link Direction#ASC} and NullHandling
		 * {@link NullHandling#NATIVE}.
		 *
		 * @param propertyPath must not be {@literal null} or empty.
		 * @since 4.1
		 */
		public static <T, P> Order asc(TypedPropertyPath<T, P> propertyPath) {
			return asc(propertyPath.toDotPath());
		}

		/**
		 * Creates a new {@link Order} instance. Takes a single property. Direction is {@link Direction#ASC} and
		 * NullHandling {@link NullHandling#NATIVE}.
		 *
		 * @param property must not be {@literal null} or empty.
		 * @since 2.0
		 */
		public static Order asc(String property) {
			return new Order(Direction.ASC, property, DEFAULT_NULL_HANDLING);
		}

		/**
		 * Creates a new {@link Order} instance. Takes a property path. Direction is {@link Direction#DESC} and NullHandling
		 * {@link NullHandling#NATIVE}.
		 *
		 * @param propertyPath must not be {@literal null} or empty.
		 * @since 4.1
		 */
		public static <T, P> Order desc(TypedPropertyPath<T, P> propertyPath) {
			return desc(propertyPath.toDotPath());
		}

		/**
		 * Creates a new {@link Order} instance. Takes a single property. Direction is {@link Direction#DESC} and
		 * NullHandling {@link NullHandling#NATIVE}.
		 *
		 * @param property must not be {@literal null} or empty.
		 * @since 2.0
		 */
		public static Order desc(String property) {
			return new Order(Direction.DESC, property, DEFAULT_NULL_HANDLING);
		}

		/**
		 * Returns the order the property shall be sorted for.
		 *
		 * @return the order the property shall be sorted for.
		 */
		public Direction getDirection() {
			return direction;
		}

		/**
		 * Returns the property to order for.
		 *
		 * @return the property to order for.
		 */
		public String getProperty() {
			return property;
		}

		/**
		 * Returns whether sorting for this property shall be ascending.
		 *
		 * @return {@literal true} if ascending, {@literal false} otherwise.
		 */
		public boolean isAscending() {
			return this.direction.isAscending();
		}

		/**
		 * Returns whether sorting for this property shall be descending.
		 *
		 * @return {@literal true} if descending, {@literal false} otherwise.
		 * @since 1.13
		 */
		public boolean isDescending() {
			return this.direction.isDescending();
		}

		/**
		 * Returns whether the sort will be case-sensitive or case-insensitive.
		 *
		 * @return {@literal true} if the sort will be case-sensitive or case-insensitive, {@literal false} otherwise.
		 */
		public boolean isIgnoreCase() {
			return ignoreCase;
		}

		/**
		 * Returns a new {@link Order} with the given {@link Direction} applied.
		 *
		 * @param direction the new direction to use.
		 * @return a new {@link Order} with the given {@link Direction} applied.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public Order with(Direction direction) {
			return new Order(direction, this.property, this.ignoreCase, this.nullHandling);
		}

		/**
		 * Returns a new {@link Order} with the reversed {@link #getDirection()} applied.
		 *
		 * @return a reversed {@link Order} with the given {@link Direction} applied.
		 * @since 3.1
		 */
		@Contract("-> new")
		public Order reverse() {
			return with(this.direction == Direction.ASC ? Direction.DESC : Direction.ASC);
		}

		/**
		 * Returns a new {@link Order} with the {@code propertyPath} applied.
		 *
		 * @param propertyPath must not be {@literal null} or empty.
		 * @return a new {@link Order} with the {@code propertyPath} applied.
		 * @since 4.1
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public <T, P> Order withProperty(TypedPropertyPath<T, P> propertyPath) {
			return withProperty(propertyPath.toDotPath());
		}

		/**
		 * Returns a new {@link Order} with the {@code property} name applied.
		 *
		 * @param property must not be {@literal null} or empty.
		 * @return a new {@link Order} with the {@code property} name applied.
		 * @since 1.13
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public Order withProperty(String property) {
			return new Order(this.direction, property, this.ignoreCase, this.nullHandling);
		}

		/**
		 * Returns a new {@link Sort} instance for the given properties using {@link #getDirection()}.
		 *
		 * @param propertyPaths properties to sort by.
		 * @return a new {@link Sort} instance for the given properties using {@link #getDirection()}.
		 * @since 4.1
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public <T> Sort withProperties(TypedPropertyPath<T, ?>... propertyPaths) {
			return Sort.by(this.direction,
					Arrays.stream(propertyPaths).map(TypedPropertyPath::toDotPath).toArray(String[]::new));
		}

		/**
		 * Returns a new {@link Sort} instance for the given properties using {@link #getDirection()}.
		 *
		 * @param properties properties to sort by.
		 * @return a new {@link Sort} instance for the given properties using {@link #getDirection()}.
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public Sort withProperties(String... properties) {
			return Sort.by(this.direction, properties);
		}

		/**
		 * Returns a new {@link Order} with case-insensitive sorting enabled.
		 *
		 * @return a new {@link Order} with case-insensitive sorting enabled.
		 */
		public Order ignoreCase() {
			return new Order(direction, property, true, nullHandling);
		}

		/**
		 * Returns a {@link Order} with the given {@link NullHandling} applied.
		 *
		 * @param nullHandling can be {@literal null}.
		 * @return a new {@link Order} with the given {@link NullHandling} applied.
		 * @since 1.8
		 */
		@Contract("_ -> new")
		@CheckReturnValue
		public Order with(NullHandling nullHandling) {
			return new Order(direction, this.property, ignoreCase, nullHandling);
		}

		/**
		 * Returns a new {@link Order} with {@link NullHandling#NULLS_FIRST} as null handling hint applied.
		 *
		 * @return a new {@link Order} with {@link NullHandling#NULLS_FIRST} as null handling hint applied.
		 * @since 1.8
		 */
		@Contract("-> new")
		public Order nullsFirst() {
			return with(NullHandling.NULLS_FIRST);
		}

		/**
		 * Returns a new {@link Order} with {@link NullHandling#NULLS_LAST} as null handling hint applied.
		 *
		 * @return a new {@link Order} with {@link NullHandling#NULLS_LAST} as null handling hint applied.
		 * @since 1.7
		 */
		@Contract("-> new")
		public Order nullsLast() {
			return with(NullHandling.NULLS_LAST);
		}

		/**
		 * Returns a new {@link Order} with {@link NullHandling#NATIVE} as null handling hint applied.
		 *
		 * @return a new {@link Order} with {@link NullHandling#NATIVE} as null handling hint applied.
		 * @since 1.7
		 */
		@Contract("-> new")
		public Order nullsNative() {
			return with(NullHandling.NATIVE);
		}

		/**
		 * Returns the used {@link NullHandling} hint, which can but may not be respected by the used datastore.
		 *
		 * @return the used {@link NullHandling} hint.
		 * @since 1.7
		 */
		public NullHandling getNullHandling() {
			return nullHandling;
		}

		@Override
		public boolean equals(@Nullable Object obj) {

			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Order that)) {
				return false;
			}

			return this.direction.equals(that.direction) && this.property.equals(that.property)
					&& this.ignoreCase == that.ignoreCase && this.nullHandling.equals(that.nullHandling);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHash(direction, property, ignoreCase, nullHandling);
		}

		@Override
		public String toString() {

			String result = String.format("%s: %s", property, direction);

			if (!NullHandling.NATIVE.equals(nullHandling)) {
				result += ", " + nullHandling;
			}

			if (ignoreCase) {
				result += ", ignoring case";
			}

			return result;
		}

	}

	/**
	 * Extension of Sort to use method handles to define properties to sort by.
	 *
	 * @author Oliver Gierke
	 * @since 2.2
	 * @soundtrack The Intersphere - Linger (The Grand Delusion)
	 * @deprecated since 4.1 in favor of {@link Sort#by(org.springframework.data.core.TypedPropertyPath...)}
	 */
	@Deprecated(since = "4.1")
	public static class TypedSort<T> extends Sort {

		private static final @Serial long serialVersionUID = -3550403511206745880L;

		private final Recorded<T> recorded;

		private TypedSort(Class<T> type) {
			this(MethodInvocationRecorder.forProxyOf(type));
		}

		private TypedSort(Recorded<T> recorded) {

			super(Collections.emptyList());
			this.recorded = recorded;
		}

		public <S> TypedSort<S> by(Function<T, S> property) {
			return new TypedSort<>(recorded.record(property));
		}

		public <S> TypedSort<S> by(Recorded.ToCollectionConverter<T, S> collectionProperty) {
			return new TypedSort<>(recorded.record(collectionProperty));
		}

		public <S> TypedSort<S> by(Recorded.ToMapConverter<T, S> mapProperty) {
			return new TypedSort<>(recorded.record(mapProperty));
		}

		@Override
		public Sort ascending() {
			return withDirection(Sort::ascending);
		}

		@Override
		public Sort descending() {
			return withDirection(Sort::descending);
		}

		private Sort withDirection(Function<Sort, Sort> direction) {

			return recorded.getPropertyPath() //
					.map(Sort::by) //
					.map(direction) //
					.orElseGet(Sort::unsorted);
		}

		@Override
		public Iterator<Order> iterator() {

			return recorded.getPropertyPath() //
					.map(Order::by) //
					.map(Collections::singleton) //
					.orElseGet(Collections::emptySet).iterator();
		}

		@Override
		public boolean isEmpty() {
			return recorded.getPropertyPath().isEmpty();
		}

		@Override
		public String toString() {

			return recorded.getPropertyPath() //
					.map(Sort::by) //
					.orElseGet(Sort::unsorted) //
					.toString();
		}
	}
}
