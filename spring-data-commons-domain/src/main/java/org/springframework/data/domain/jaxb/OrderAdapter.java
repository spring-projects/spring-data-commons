/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.domain.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.domain.jaxb.SpringDataJaxb.OrderDto;
import org.springframework.lang.Contract;

/**
 * XmlAdapter to convert {@link Order} instances into {@link OrderDto}s and vice versa.
 *
 * @author Oliver Gierke
 */
public class OrderAdapter extends XmlAdapter<OrderDto, Order> {

	public static final OrderAdapter INSTANCE = new OrderAdapter();

	@Override
	public @Nullable OrderDto marshal(@Nullable Order order) {

		if (order == null) {
			return null;
		}

		OrderDto dto = new OrderDto();
		dto.direction = order.getDirection();
		dto.property = order.getProperty();
		return dto;
	}

	@Contract("null -> null")
	@Override
	public @Nullable Order unmarshal(@Nullable OrderDto source) {

		if (source == null) {
			return null;
		}

		Sort.Direction direction = source.direction;
		String property = source.property;

		if (direction == null || property == null) {
			return null;
		}

		return new Order(direction, property);
	}
}
