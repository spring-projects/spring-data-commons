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

import static java.lang.String.*;
import static java.util.regex.Pattern.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;


/**
 * Helper class to split a method name into all of its logical parts (prefix,
 * properties, postfix).
 * 
 * @author Oliver Gierke
 */
public class PartSource {

    private static final String ORDER_BY = "OrderBy";
    private static final String[] PREFIXES = new String[] { "findBy", "find",
            "readBy", "read", "getBy", "get" };
    private static final String PREFIX_TEMPLATE = "^%s(?=[A-Z]).*";
    private static final String KEYWORD_TEMPLATE = "(%s)(?=[A-Z])";

    private final String cleanedUpString;
    private final OrderBySource orderBySource;


    public PartSource(String methodName) {

        String removedPrefixes = strip(methodName);

        String[] parts = split(removedPrefixes, ORDER_BY);

        if (parts.length > 2) {
            throw new IllegalArgumentException(
                    "OrderBy must not be used more than once in a method name!");
        }

        this.cleanedUpString = parts[0];
        this.orderBySource =
                parts.length == 2 ? getOrderBySourceFor(parts[1]) : null;
    }


    public OrderBySource getOrderBySource() {

        return orderBySource;
    }


    public boolean hasOrderByClause() {

        return orderBySource != null;
    }


    protected OrderBySource getOrderBySourceFor(String postfix) {

        return new OrderBySource(postfix);
    }


    /**
     * Returns an iterator over all the {@link PartSource}s created by spliting
     * up the current one with the given keyword.
     * 
     * @param keyword
     * @return
     */
    public Iterator<PartSource> getParts(String keyword) {

        List<PartSource> parts = new ArrayList<PartSource>();
        for (String part : split(cleanedUpString, keyword)) {
            parts.add(new PartSource(part));
        }

        return parts.iterator();
    }


    public String cleanedUp() {

        return cleanedUpString;
    }


    /**
     * Strips a prefix from the given method name if it starts with one of
     * {@value #PREFIXES}.
     * 
     * @param methodName
     * @return
     */
    private String strip(String methodName) {

        for (String prefix : PREFIXES) {

            String regex = format(PREFIX_TEMPLATE, prefix);
            if (methodName.matches(regex)) {
                return methodName.substring(prefix.length());
            }
        }

        return methodName;
    }


    /**
     * Splits the given text at the given keywords. Expects camelcase style to
     * only match concrete keywords and not derivatives of it.
     * 
     * @param text
     * @param keyword
     * @return
     */
    private String[] split(String text, String keyword) {

        String regex = format(KEYWORD_TEMPLATE, keyword);

        Pattern pattern = compile(regex);
        return pattern.split(text);
    }
}