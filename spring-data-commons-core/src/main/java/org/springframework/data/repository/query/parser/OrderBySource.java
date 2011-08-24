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
package org.springframework.data.repository.query.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.StringUtils;

/**
 * Simple helper class to create a {@link Sort} instance from a method name end. It expects the last part of the method
 * name to be given and supports lining up multiple properties ending with the sorting direction. So the following
 * method ends are valid: {@code LastnameUsernameDesc}, {@code LastnameAscUsernameDesc}.
 *
 * @author Oliver Gierke
 */
public class OrderBySource {

	private final String BLOCK_SPLIT = "(?<=Asc|Desc)(?=[A-Z])";
	private final Pattern DIRECTION_SPLIT = Pattern.compile("(.+)(Asc|Desc)$");

	private final List<Order> orders;

	public OrderBySource(String clause) {

		this(clause, null);
	}

	public OrderBySource(String clause, Class<?> domainClass) {

		this.orders = new ArrayList<Sort.Order>();
		for (String part : clause.split(BLOCK_SPLIT)) {
			Matcher matcher = DIRECTION_SPLIT.matcher(part);
			if (!matcher.find()) {
				throw new IllegalArgumentException(String.format("Invalid order syntax for part %s!", part));
			}
			Direction direction = Direction.fromString(matcher.group(2));
			this.orders.add(createOrder(matcher.group(1), direction, domainClass));
		}
	}

	/**
	 * Creates an {@link Order} instance from the given property source, direction and domain class. If the domain class
	 * is given, we will use it for nested property traversal checks.
	 *
	 * @param propertySource
	 * @param direction
	 * @param domainClass can be {@literal null}.
	 * @return
	 * @see Property#from(String, Class)
	 */
	private Order createOrder(String propertySource, Direction direction, Class<?> domainClass) {

		if (null == domainClass) {
			return new Order(direction, StringUtils.uncapitalize(propertySource));
		}
		Property property = Property.from(propertySource, domainClass);
		return new Order(direction, property.toDotPath());
	}

	public Sort toSort() {

		return this.orders.isEmpty() ? null : new Sort(this.orders);
	}

	@Override
	public String toString() {

		return "Order By " + StringUtils.collectionToDelimitedString(orders, ", ");
	}
}