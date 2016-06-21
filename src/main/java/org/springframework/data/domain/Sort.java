/*
 * Copyright 2008-2016 the original author or authors.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Sort option for queries. You have to provide at least a list of properties to sort for that must not include
 * {@literal null} or empty strings. The direction defaults to {@link Sort#DEFAULT_DIRECTION}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Sort implements Iterable<org.springframework.data.domain.Sort.Order>, Serializable {

	private static final long serialVersionUID = 5737186511678863905L;

	private static final Sort UNSORTED = new Sort(new Order[0]);

	public static final Direction DEFAULT_DIRECTION = Direction.ASC;

	private final List<Order> orders;

	/**
	 * Creates a new {@link Sort} instance using the given {@link Order}s.
	 * 
	 * @param orders must not be {@literal null}.
	 */
	@Deprecated
	public Sort(Order... orders) {
		this(Arrays.asList(orders));
	}

	/**
	 * Creates a new {@link Sort} instance.
	 * 
	 * @param orders must not be {@literal null} or contain {@literal null}.
	 * @deprecated see {@link Sort#by(List)}
	 */
	@Deprecated
	public Sort(List<Order> orders) {

		Assert.notNull(orders, "Orders must not be null!");

		this.orders = Collections.unmodifiableList(orders);
	}

	/**
	 * Creates a new {@link Sort} instance. Order defaults to {@value Direction#ASC}.
	 * 
	 * @param properties must not be {@literal null} or contain {@literal null} or empty strings
	 * @deprecated use {@link Sort#by(String...)}
	 */
	@Deprecated
	public Sort(String... properties) {
		this(DEFAULT_DIRECTION, properties);
	}

	/**
	 * Creates a new {@link Sort} instance.
	 * 
	 * @param direction defaults to {@linke Sort#DEFAULT_DIRECTION} (for {@literal null} cases, too)
	 * @param properties must not be {@literal null}, empty or contain {@literal null} or empty strings.
	 */
	public Sort(Direction direction, String... properties) {
		this(direction, properties == null ? new ArrayList<>() : Arrays.asList(properties));
	}

	/**
	 * Creates a new {@link Sort} instance.
	 * 
	 * @param direction defaults to {@linke Sort#DEFAULT_DIRECTION} (for {@literal null} cases, too)
	 * @param properties must not be {@literal null} or contain {@literal null} or empty strings.
	 */
	public Sort(Direction direction, List<String> properties) {

		if (properties == null || properties.isEmpty()) {
			throw new IllegalArgumentException("You have to provide at least one property to sort by!");
		}

		this.orders = new ArrayList<>(properties.size());

		for (String property : properties) {
			this.orders.add(new Order(direction, property));
		}
	}

	public static Sort by(String... properties) {
		return properties.length == 0 ? Sort.unsorted() : new Sort(properties);
	}

	public static Sort by(List<Order> orders) {
		return orders.isEmpty() ? Sort.unsorted() : new Sort(orders);
	}

	public static Sort by(Order... orders) {
		return new Sort(orders);
	}

	public static Sort unsorted() {
		return UNSORTED;
	}

	public Sort descending() {
		return withDirection(Direction.DESC);
	}

	public Sort ascending() {
		return withDirection(Direction.ASC);
	}

	private Sort withDirection(Direction direction) {
		return new Sort(orders.stream().map(it -> new Order(direction, it.getProperty())).collect(Collectors.toList()));
	}

	public boolean isSorted() {
		return !orders.isEmpty();
	}

	/**
	 * Returns a new {@link Sort} consisting of the {@link Order}s of the current {@link Sort} combined with the given
	 * ones.
	 * 
	 * @param sort can be {@literal null}.
	 * @return
	 */
	public Sort and(Sort sort) {

		if (sort == null) {
			return this;
		}

		ArrayList<Order> these = new ArrayList<>(this.orders);

		for (Order order : sort) {
			these.add(order);
		}

		return new Sort(these);
	}

	/**
	 * Returns the order registered for the given property.
	 * 
	 * @param property
	 * @return
	 */
	public Order getOrderFor(String property) {

		for (Order order : this) {
			if (order.getProperty().equals(property)) {
				return order;
			}
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Order> iterator() {
		return this.orders.iterator();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Sort)) {
			return false;
		}

		Sort that = (Sort) obj;

		return this.orders.equals(that.orders);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;
		result = 31 * result + orders.hashCode();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return orders.isEmpty() ? "UNSORTED" : StringUtils.collectionToCommaDelimitedString(orders);
	}

	/**
	 * Enumeration for sort directions.
	 * 
	 * @author Oliver Gierke
	 */
	public static enum Direction {

		ASC, DESC;

		/**
		 * Returns whether the direction is ascending.
		 * 
		 * @return
		 * @since 1.13
		 */
		public boolean isAscending() {
			return this.equals(ASC);
		}

		/**
		 * Returns whether the direction is descending.
		 * 
		 * @return
		 * @since 1.13
		 */
		public boolean isDescending() {
			return this.equals(DESC);
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} value.
		 * 
		 * @param value
		 * @throws IllegalArgumentException in case the given value cannot be parsed into an enum value.
		 * @return
		 */
		public static Direction fromString(String value) {

			try {
				return Direction.valueOf(value.toUpperCase(Locale.US));
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).", value), e);
			}
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} or null if it cannot be parsed into an enum
		 * value.
		 * 
		 * @param value
		 * @return
		 */
		public static Optional<Direction> fromOptionalString(String value) {

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
	public static enum NullHandling {

		/**
		 * Lets the data store decide what to do with nulls.
		 */
		NATIVE,

		/**
		 * A hint to the used data store to order entries with null values before non null entries.
		 */
		NULLS_FIRST,

		/**
		 * A hint to the used data store to order entries with null values after non null entries.
		 */
		NULLS_LAST;
	}

	/**
	 * PropertyPath implements the pairing of an {@link Direction} and a property. It is used to provide input for
	 * {@link Sort}
	 * 
	 * @author Oliver Gierke
	 * @author Kevin Raymond
	 */
	public static class Order implements Serializable {

		private static final long serialVersionUID = 1522511010900108987L;
		private static final boolean DEFAULT_IGNORE_CASE = false;

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
		public Order(Direction direction, String property) {
			this(direction, property, DEFAULT_IGNORE_CASE, null);
		}

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 * 
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
		 * @param property must not be {@literal null} or empty.
		 * @param nullHandling can be {@literal null}, will default to {@link NullHandling#NATIVE}.
		 */
		public Order(Direction direction, String property, NullHandling nullHandlingHint) {
			this(direction, property, DEFAULT_IGNORE_CASE, nullHandlingHint);
		}

		/**
		 * Creates a new {@link Order} instance. Takes a single property. Direction defaults to
		 * {@link Sort#DEFAULT_DIRECTION}.
		 * 
		 * @param property must not be {@literal null} or empty.
		 */
		public Order(String property) {
			this(DEFAULT_DIRECTION, property);
		}

		/**
		 * Creates a new {@link Order} instance. if order is {@literal null} then order defaults to
		 * {@link Sort#DEFAULT_DIRECTION}
		 * 
		 * @param direction can be {@literal null}, will default to {@link Sort#DEFAULT_DIRECTION}
		 * @param property must not be {@literal null} or empty.
		 * @param ignoreCase true if sorting should be case insensitive. false if sorting should be case sensitive.
		 * @param nullHandling can be {@literal null}, will default to {@link NullHandling#NATIVE}.
		 * @since 1.7
		 */
		private Order(Direction direction, String property, boolean ignoreCase, NullHandling nullHandling) {

			if (!StringUtils.hasText(property)) {
				throw new IllegalArgumentException("Property must not null or empty!");
			}

			this.direction = direction == null ? DEFAULT_DIRECTION : direction;
			this.property = property;
			this.ignoreCase = ignoreCase;
			this.nullHandling = nullHandling == null ? NullHandling.NATIVE : nullHandling;
		}

		/**
		 * Returns the order the property shall be sorted for.
		 * 
		 * @return
		 */
		public Direction getDirection() {
			return direction;
		}

		/**
		 * Returns the property to order for.
		 * 
		 * @return
		 */
		public String getProperty() {
			return property;
		}

		/**
		 * Returns whether sorting for this property shall be ascending.
		 * 
		 * @return
		 */
		public boolean isAscending() {
			return this.direction.isAscending();
		}

		/**
		 * Returns whether sorting for this property shall be descending.
		 * 
		 * @return
		 * @since 1.13
		 */
		public boolean isDescending() {
			return this.direction.isDescending();
		}

		/**
		 * Returns whether or not the sort will be case sensitive.
		 * 
		 * @return
		 */
		public boolean isIgnoreCase() {
			return ignoreCase;
		}

		/**
		 * Returns a new {@link Order} with the given {@link Direction}.
		 * 
		 * @param direction
		 * @return
		 */
		public Order with(Direction direction) {
			return new Order(direction, this.property, this.ignoreCase, this.nullHandling);
		}

		/**
		 * Returns a new {@link Order}
		 * 
		 * @param property must not be {@literal null} or empty.
		 * @return
		 * @since 1.13
		 */
		public Order withProperty(String property) {
			return new Order(this.direction, property, this.ignoreCase, this.nullHandling);
		}

		/**
		 * Returns a new {@link Sort} instance for the given properties.
		 * 
		 * @param properties
		 * @return
		 */
		public Sort withProperties(String... properties) {
			return new Sort(this.direction, properties);
		}

		/**
		 * Returns a new {@link Order} with case insensitive sorting enabled.
		 * 
		 * @return
		 */
		public Order ignoreCase() {
			return new Order(direction, property, true, nullHandling);
		}

		/**
		 * Returns a {@link Order} with the given {@link NullHandling}.
		 * 
		 * @param nullHandling can be {@literal null}.
		 * @return
		 * @since 1.8
		 */
		public Order with(NullHandling nullHandling) {
			return new Order(direction, this.property, ignoreCase, nullHandling);
		}

		/**
		 * Returns a {@link Order} with {@link NullHandling#NULLS_FIRST} as null handling hint.
		 * 
		 * @return
		 * @since 1.8
		 */
		public Order nullsFirst() {
			return with(NullHandling.NULLS_FIRST);
		}

		/**
		 * Returns a {@link Order} with {@link NullHandling#NULLS_LAST} as null handling hint.
		 * 
		 * @return
		 * @since 1.7
		 */
		public Order nullsLast() {
			return with(NullHandling.NULLS_LAST);
		}

		/**
		 * Returns a {@link Order} with {@link NullHandling#NATIVE} as null handling hint.
		 * 
		 * @return
		 * @since 1.7
		 */
		public Order nullsNative() {
			return with(NullHandling.NATIVE);
		}

		/**
		 * Returns the used {@link NullHandling} hint, which can but may not be respected by the used datastore.
		 * 
		 * @return
		 * @since 1.7
		 */
		public NullHandling getNullHandling() {
			return nullHandling;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = 17;

			result = 31 * result + direction.hashCode();
			result = 31 * result + property.hashCode();
			result = 31 * result + (ignoreCase ? 1 : 0);
			result = 31 * result + nullHandling.hashCode();

			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Order)) {
				return false;
			}

			Order that = (Order) obj;

			return this.direction.equals(that.direction) && this.property.equals(that.property)
					&& this.ignoreCase == that.ignoreCase && this.nullHandling.equals(that.nullHandling);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
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
}
