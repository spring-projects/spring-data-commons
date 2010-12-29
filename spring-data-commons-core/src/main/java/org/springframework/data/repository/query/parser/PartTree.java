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

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.util.Assert;


/**
 * Class to parse a {@link String} into a tree or {@link OrPart}s consisting of
 * simple {@link Part} instances in turn. Takes a domain class as well to
 * validate that each of the {@link Part}s are refering to a property of the
 * domain class. The {@link PartTree} can then be used to build queries based on
 * its API instead of parsing the method name for each query execution.
 * 
 * @author Oliver Gierke
 */
public class PartTree implements Iterable<OrPart> {

    private static final String ORDER_BY = "OrderBy";
    private static final String[] PREFIXES = new String[] { "findBy", "find",
            "readBy", "read", "getBy", "get" };
    private static final String PREFIX_TEMPLATE = "^%s(?=[A-Z]).*";
    private static final String KEYWORD_TEMPLATE = "(%s)(?=[A-Z])";

    private final OrderBySource orderBySource;
    private final List<OrPart> nodes = new ArrayList<PartTree.OrPart>();


    /**
     * Creates a new {@link PartTree} by parsing the given {@link String}
     * 
     * @param source the {@link String} to parse
     * @param domainClass the domain class to check indiviual parts against to
     *            ensure they refer to a property of the class
     */
    public PartTree(String source, Class<?> domainClass) {

        Assert.notNull(source);
        Assert.notNull(domainClass);

        String foo = strip(source);
        String[] parts = split(foo, ORDER_BY);

        if (parts.length > 2) {
            throw new IllegalArgumentException(
                    "OrderBy must not be used more than once in a method name!");
        }

        buildTree(parts[0], domainClass);
        this.orderBySource =
                parts.length == 2 ? new OrderBySource(parts[1]) : null;

    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<OrPart> iterator() {

        return nodes.iterator();
    }


    private void buildTree(String source, Class<?> domainClass) {

        String[] split = split(source, "Or");

        for (String part : split) {
            nodes.add(new OrPart(part, domainClass));
        }
    }


    /**
     * Returns the {@link Sort} specification parsed from the source.
     * 
     * @return
     */
    public Sort getSort() {

        return orderBySource == null ? null : orderBySource.toSort();
    }


    /**
     * Splits the given text at the given keywords. Expects camelcase style to
     * only match concrete keywords and not derivatives of it.
     * 
     * @param text
     * @param keyword
     * @return
     */
    private static String[] split(String text, String keyword) {

        String regex = format(KEYWORD_TEMPLATE, keyword);

        Pattern pattern = compile(regex);
        return pattern.split(text);
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
     * A part of the parsed source that results from splitting up the resource
     * ar {@literal Or} keywords. Consists of {@link Part}s that have to be
     * concatenated by {@literal And}.
     * 
     * @author Oliver Gierke
     */
    public static class OrPart implements Iterable<Part> {

        private final List<Part> children = new ArrayList<Part>();


        /**
         * Creates a new {@link OrPart}.
         * 
         * @param source the source to split up into {@literal And} parts in
         *            turn.
         * @param domainClass the domain class to check the resulting
         *            {@link Part}s against.
         */
        OrPart(String source, Class<?> domainClass) {

            String[] split = split(source, "And");

            for (String part : split) {
                children.add(new Part(part, domainClass));
            }
        }


        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<Part> iterator() {

            return children.iterator();
        }
    }
}
