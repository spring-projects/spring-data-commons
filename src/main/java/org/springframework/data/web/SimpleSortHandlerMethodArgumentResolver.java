/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.web;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link HandlerMethodArgumentResolver} to automatically create {@link Sort} instances from request parameters or
 * {@link SortDefault} annotations.
 *
 * @since 1.6
 * @author Muhammad Ichsan
 */
public class SimpleSortHandlerMethodArgumentResolver extends SortHandlerMethodArgumentResolver {

    private static final String DEFAULT_ASCENDING_SIGN = "";

    private String ascendingSign = DEFAULT_ASCENDING_SIGN;

    /**
     * Configures the sign used to mark ascending property. Defaults to {@code }, so a
     * qualified sort property would look like {@code qualifier_sort}.
     *
     * @param ascendingSign must not be {@literal null} or empty.
     */
    public void setAscendingSign(String ascendingSign) {
        Assert.hasText(ascendingSign);
        this.ascendingSign = ascendingSign;
    }

    @Override
    Sort parseParameterIntoSort(String[] source, String delimiter) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Parses the given sort expressions into a {@link Sort} instance. The implementation expects the sources to be a
     * concatenation of Strings using the given delimiter. If the last element can be parsed into a {@link Direction} it's
     * considered a {@link Direction} and a simple property otherwise.
     *
     * @param part will never be {@literal null}.
     * @param delimiter the delimiter to be used to split up the source elements, will never be {@literal null}.
     * @return
     */
    Sort parseParameterIntoSort(String part, String delimiter) {

        List<Order> allOrders = new ArrayList<Order>();

        if (part != null) {
            String[] elements = part.split(delimiter);

            for (int i = 0; i < elements.length; i++) {
                String property = elements[i];

                Direction direction = null;
                if (property.startsWith("-")) {
                    property = property.substring(1, property.length());
                    direction = Direction.DESC;
                } else if (ascendingSign.isEmpty() || property.startsWith(ascendingSign)) {
                    property = property.substring(ascendingSign.length(), property.length());
                    direction = Direction.ASC;
                }

                if (!StringUtils.hasText(property)) {
                    continue;
                }

                if (direction != null) {
                    allOrders.add(new Order(direction, property));
                }
            }
        }

        return allOrders.isEmpty() ? null : new Sort(allOrders);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
     */
    @Override
    public Sort resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        String directionParameter = webRequest.getParameter(getSortParameter(parameter));

        // No parameter
        if (directionParameter == null) {
            return getDefaultFromAnnotationOrFallback(parameter);
        }

        // Single empty parameter, e.g "sort="
        if (!StringUtils.hasText(directionParameter)) {
            return getDefaultFromAnnotationOrFallback(parameter);
        }

        return parseParameterIntoSort(directionParameter, propertyDelimiter);
    }

}
