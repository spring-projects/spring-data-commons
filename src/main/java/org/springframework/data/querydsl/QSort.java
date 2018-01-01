/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.querydsl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;

/**
 * Sort option for queries that wraps a Querydsl {@link OrderSpecifier}.
 *
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class QSort extends Sort implements Serializable {

	private static final long serialVersionUID = -6701117396842171930L;
	private static final QSort UNSORTED = new QSort();

	private final List<OrderSpecifier<?>> orderSpecifiers;

	/**
	 * Creates a new {@link QSort} instance with the given {@link OrderSpecifier}s.
	 *
	 * @param orderSpecifiers must not be {@literal null} .
	 */
	public QSort(OrderSpecifier<?>... orderSpecifiers) {
		this(Arrays.asList(orderSpecifiers));
	}

	/**
	 * Creates a new {@link QSort} instance with the given {@link OrderSpecifier}s.
	 *
	 * @param orderSpecifiers must not be {@literal null}.
	 */
	@SuppressWarnings("deprecation")
	public QSort(List<OrderSpecifier<?>> orderSpecifiers) {

		super(toOrders(orderSpecifiers));

		this.orderSpecifiers = orderSpecifiers;
	}

	public static QSort by(OrderSpecifier<?>... orderSpecifiers) {
		return new QSort(orderSpecifiers);
	}

	public static QSort unsorted() {
		return UNSORTED;
	}

	/**
	 * Converts the given {@link OrderSpecifier}s into a list of {@link Order}s.
	 *
	 * @param orderSpecifiers must not be {@literal null} or empty.
	 * @return
	 */
	private static List<Order> toOrders(List<OrderSpecifier<?>> orderSpecifiers) {

		Assert.notNull(orderSpecifiers, "Order specifiers must not be null!");

		return orderSpecifiers.stream().map(QSort::toOrder).collect(Collectors.toList());
	}

	/**
	 * Converts the given {@link OrderSpecifier} into an {@link Order}.
	 *
	 * @param orderSpecifier must not be {@literal null}.
	 * @return
	 */
	private static Order toOrder(OrderSpecifier<?> orderSpecifier) {

		Assert.notNull(orderSpecifier, "Order specifier must not be null!");

		Expression<?> target = orderSpecifier.getTarget();

		Object targetElement = target instanceof Path ? preparePropertyPath((Path<?>) target) : target;

		Assert.notNull(targetElement, "Target element must not be null!");

		return Order.by(targetElement.toString()).with(orderSpecifier.isAscending() ? Direction.ASC : Direction.DESC);
	}

	/**
	 * @return the orderSpecifier
	 */
	public List<OrderSpecifier<?>> getOrderSpecifiers() {
		return orderSpecifiers;
	}

	/**
	 * Returns a new {@link QSort} consisting of the {@link OrderSpecifier}s of the current {@code QSort} combined with
	 * the ones from the given {@code QSort}.
	 *
	 * @param sort can be {@literal null}.
	 * @return
	 */
	public QSort and(QSort sort) {
		return sort == null ? this : and(sort.getOrderSpecifiers());
	}

	/**
	 * Returns a new {@link QSort} consisting of the {@link OrderSpecifier}s of the current {@link QSort} combined with
	 * the given ones.
	 *
	 * @param orderSpecifiers must not be {@literal null} or empty.
	 * @return
	 */
	public QSort and(List<OrderSpecifier<?>> orderSpecifiers) {

		Assert.notEmpty(orderSpecifiers, "OrderSpecifiers must not be null or empty!");

		List<OrderSpecifier<?>> newOrderSpecifiers = new ArrayList<>(this.orderSpecifiers);
		newOrderSpecifiers.addAll(orderSpecifiers);

		return new QSort(newOrderSpecifiers);
	}

	/**
	 * Returns a new {@link QSort} consisting of the {@link OrderSpecifier}s of the current {@link QSort} combined with
	 * the given ones.
	 *
	 * @param orderSpecifiers must not be {@literal null} or empty.
	 * @return
	 */
	public QSort and(OrderSpecifier<?>... orderSpecifiers) {

		Assert.notEmpty(orderSpecifiers, "OrderSpecifiers must not be null or empty!");
		return and(Arrays.asList(orderSpecifiers));
	}

	/**
	 * Recursively creates a dot-separated path for the property path.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private static String preparePropertyPath(Path<?> path) {

		Path<?> root = path.getRoot();

		return root == null || path.equals(root) ? path.toString()
				: path.toString().substring(root.toString().length() + 1);
	}
}
