/*
 * Copyright 2008-2010 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.springframework.util.StringUtils;


/**
 * Sort option for queries. You have to provide at least a list of properties to
 * sort for that must not include {@code null} or empty strings. The direction
 * defaults to {@value Sort#DEFAULT_DIRECTION}.
 *
 * @author Oliver Gierke
 */
public class Sort implements
		Iterable<org.springframework.data.domain.Sort.Order>, Serializable {

	private static final long serialVersionUID = 5737186511678863905L;
	public static final Direction DEFAULT_DIRECTION = Direction.ASC;

	private List<Order> orders;


	public Sort(Order... orders) {

		this(Arrays.asList(orders));
	}


	/**
	 * Creates a new {@link Sort} instance.
	 *
	 * @param orders must not be {@literal null} or contain {@literal null} or
	 *               empty strings
	 */
	public Sort(List<Order> orders) {

		if (null == orders || orders.isEmpty()) {
			throw new IllegalArgumentException(
					"You have to provide at least one sort property to sort by!");
		}

		this.orders = orders;
	}


	/**
	 * Creates a new {@link Sort} instance. Order defaults to
	 * {@value Direction#ASC}.
	 *
	 * @param properties must not be {@literal null} or contain {@literal null}
	 *                   or empty strings
	 */
	public Sort(String... properties) {

		this(DEFAULT_DIRECTION, properties);
	}


	/**
	 * Creates a new {@link Sort} instance.
	 *
	 * @param direction	defaults to {@value Sort#DEFAULT_DIRECTION} (for
	 *                   {@literal null} cases, too)
	 * @param properties must not be {@literal null} or contain {@literal null}
	 *                   or empty strings
	 */
	public Sort(Direction direction, String... properties) {

		this(direction, properties == null ? new ArrayList<String>() : Arrays
				.asList(properties));
	}


	/**
	 * Creates a new {@link Sort} instance.
	 *
	 * @param direction
	 * @param properties
	 */
	public Sort(Direction direction, List<String> properties) {

		if (properties == null || properties.isEmpty()) {
			throw new IllegalArgumentException(
					"You have to provide at least one property to sort by!");
		}

		this.orders = new ArrayList<Order>(properties.size());

		for (String property : properties) {
			this.orders.add(new Order(direction, property));
		}
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
			 *
			 * @see java.lang.Iterable#iterator()
			 */
	public Iterator<Order> iterator() {

		return this.orders.iterator();
	}


	/*
			 * (non-Javadoc)
			 *
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
			 *
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
			 *
			 * @see java.lang.Object#toString()
			 */
	@Override
	public String toString() {

		return StringUtils.collectionToCommaDelimitedString(orders);
	}

	/**
	 * Enumeration for sort directions.
	 *
	 * @author Oliver Gierke
	 */
	public static enum Direction {

		ASC, DESC;

		/**
		 * Returns the {@link Direction} enum for the given {@link String}
		 * value.
		 *
		 * @param value
		 * @return
		 */
		public static Direction fromString(String value) {

			try {
				return Direction.valueOf(value.toUpperCase(Locale.US));
			} catch (Exception e) {
				throw new IllegalArgumentException(
						String.format(
								"Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).",
								value), e);
			}
		}
	}

	/**
	 * Property implements the pairing of an {@code Order} and a property. It is
	 * used to provide input for {@link Sort}
	 *
	 * @author Oliver Gierke
	 */
	public static class Order {

		private final Direction direction;
		private final String property;


		/**
		 * Creates a new {@link Order} instance. if order is {@literal null}
		 * then order defaults to {@value Sort#DEFAULT_DIRECTION}
		 *
		 * @param direction can be {@code null}
		 * @param property	must not be {@code null} or empty
		 */
		public Order(Direction direction, String property) {

			if (property == null || "".equals(property.trim())) {
				throw new IllegalArgumentException(
						"Property must not null or empty!");
			}

			this.direction = direction == null ? DEFAULT_DIRECTION : direction;
			this.property = property;
		}


		/**
		 * Creates a new {@link Order} instance. Takes a single property. Order
		 * defaults to {@value Sort.DEFAULT_ORDER}
		 *
		 * @param property - must not be {@code null} or empty
		 */
		public Order(String property) {

			this(DEFAULT_DIRECTION, property);
		}


		public static List<Order> create(Direction direction,
																		 Iterable<String> properties) {

			List<Order> orders = new ArrayList<Sort.Order>();
			for (String property : properties) {
				orders.add(new Order(direction, property));
			}
			return orders;
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

			return this.direction.equals(Direction.ASC);
		}


		/**
		 * Returns a new {@link Order} with the given {@link Order}.
		 *
		 * @param order
		 * @return
		 */
		public Order with(Direction order) {

			return new Order(order, this.property);
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


		/*
						 * (non-Javadoc)
						 *
						 * @see java.lang.Object#hashCode()
						 */
		@Override
		public int hashCode() {

			int result = 17;

			result = 31 * result + direction.hashCode();
			result = 31 * result + property.hashCode();

			return result;
		}


		/*
						 * (non-Javadoc)
						 *
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

			return this.direction.equals(that.direction)
					&& this.property.equals(that.property);
		}


		/*
						 * (non-Javadoc)
						 *
						 * @see java.lang.Object#toString()
						 */
		@Override
		public String toString() {

			return String.format("%s: %s", property, direction);
		}
	}
}
