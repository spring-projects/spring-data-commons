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

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Simple helper class to create a {@link Sort} instance from a method name end.
 * It expects the last part of the method name to be given and supports lining
 * up multiple properties ending with the sorting direction. So the following
 * method ends are valid: {@code LastnameUsernameDesc},
 * {@code LastnameAscUsernameDesc}.
 * 
 * @author Oliver Gierke
 */
public class OrderBySource {

    private final List<Order> orders;


    public OrderBySource(String clause) {

        this.orders = new ArrayList<Sort.Order>();
        List<String> properties = new ArrayList<String>();

        for (String part : clause.split("(?<=[a-z])(?=[A-Z])")) {

            Direction direction = defaultedFrom(part);

            if (direction == null) {
                properties.add(StringUtils.uncapitalize(part));
            } else {
                Assert.notEmpty(
                        properties,
                        "Invalid order syntax! You have to provide at least one property before the sort direction.");
                orders.addAll(Order.create(direction, properties));
                properties.clear();
            }
        }
    }


    /**
     * Tries to resolve a {@link Direction} for the given {@link String}.
     * Returns {@literal null} if resolving fails.
     * 
     * @param candidate
     * @return
     */
    private Direction defaultedFrom(String candidate) {

        try {
            return Direction.fromString(candidate);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    public Sort toSort() {

        return this.orders.isEmpty() ? null : new Sort(this.orders);
    }
}