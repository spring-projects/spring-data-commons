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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Parser to populate the given
 * {@link ClassPathScanningCandidateComponentProvider} with {@link TypeFilter}s
 * parsed from the given {@link Element}'s children.
 * 
 * @author Oliver Gierke
 */
class TypeFilterParser {

    private static final String FILTER_TYPE_ATTRIBUTE = "type";
    private static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";

    private final ClassLoader classLoader;
    private final ReaderContext readerContext;


    /**
     * Creates a new {@link TypeFilterParser} with the given {@link ClassLoader}
     * and {@link ReaderContext}.
     * 
     * @param classLoader
     * @param readerContext
     */
    public TypeFilterParser(ClassLoader classLoader, ReaderContext readerContext) {

        this.classLoader = classLoader;
        this.readerContext = readerContext;
    }


    /**
     * Parses include and exclude filters form the given {@link Element}'s child
     * elements and populates the given
     * {@link ClassPathScanningCandidateComponentProvider} with the according
     * {@link TypeFilter}s.
     * 
     * @param element
     * @param scanner
     */
    public void parseFilters(Element element,
            ClassPathScanningCandidateComponentProvider scanner) {

        parseTypeFilters(element, scanner, Type.INCLUDE);
        parseTypeFilters(element, scanner, Type.EXCLUDE);
    }


    private void parseTypeFilters(Element element,
            ClassPathScanningCandidateComponentProvider scanner, Type type) {

        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            Element childElement = type.getElement(node);

            if (childElement != null) {

                try {

                    type.addFilter(
                            createTypeFilter((Element) node, classLoader),
                            scanner);

                } catch (RuntimeException e) {
                    readerContext.error(e.getMessage(),
                            readerContext.extractSource(element), e.getCause());
                }
            }
        }
    }


    protected TypeFilter createTypeFilter(Element element,
            ClassLoader classLoader) {

        String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
        String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);

        try {

            FilterType filter = FilterType.fromString(filterType);
            return filter.getFilter(expression, classLoader);

        } catch (ClassNotFoundException ex) {
            throw new FatalBeanException("Type filter class not found: "
                    + expression, ex);
        }
    }

    /**
     * Enum representing all the filter types available for {@code include} and
     * {@code exclude} elements. This acts as factory for {@link TypeFilter}
     * instances.
     * 
     * @see #getFilter(String, ClassLoader)
     * @author Oliver Gierke
     */
    private static enum FilterType {

        ANNOTATION {

            @Override
            @SuppressWarnings("unchecked")
            public TypeFilter getFilter(String expression,
                    ClassLoader classLoader) throws ClassNotFoundException {

                return new AnnotationTypeFilter(
                        (Class<Annotation>) classLoader.loadClass(expression));
            }
        },

        ASSIGNABLE {

            @Override
            public TypeFilter getFilter(String expression,
                    ClassLoader classLoader) throws ClassNotFoundException {

                return new AssignableTypeFilter(
                        classLoader.loadClass(expression));
            }

        },

        ASPECTJ {

            @Override
            public TypeFilter getFilter(String expression,
                    ClassLoader classLoader) {

                return new AspectJTypeFilter(expression, classLoader);
            }

        },

        REGEX {

            @Override
            public TypeFilter getFilter(String expression,
                    ClassLoader classLoader) {

                return new RegexPatternTypeFilter(Pattern.compile(expression));
            }

        },

        CUSTOM {

            @Override
            public TypeFilter getFilter(String expression,
                    ClassLoader classLoader) throws ClassNotFoundException {

                Class<?> filterClass = classLoader.loadClass(expression);
                if (!TypeFilter.class.isAssignableFrom(filterClass)) {
                    throw new IllegalArgumentException(
                            "Class is not assignable to ["
                                    + TypeFilter.class.getName() + "]: "
                                    + expression);
                }
                return (TypeFilter) BeanUtils.instantiateClass(filterClass);
            }
        };

        /**
         * Returns the {@link TypeFilter} for the given expression and
         * {@link ClassLoader}.
         * 
         * @param expression
         * @param classLoader
         * @return
         * @throws ClassNotFoundException
         */
        abstract TypeFilter getFilter(String expression, ClassLoader classLoader)
                throws ClassNotFoundException;


        /**
         * Returns the {@link FilterType} for the given type as {@link String}.
         * 
         * @param typeString
         * @return
         * @throws IllegalArgumentException if no {@link FilterType} could be
         *             found for the given argument.
         */
        static FilterType fromString(String typeString) {

            for (FilterType filter : FilterType.values()) {
                if (filter.name().equalsIgnoreCase(typeString)) {
                    return filter;
                }
            }

            throw new IllegalArgumentException("Unsupported filter type: "
                    + typeString);
        }
    }

    private static enum Type {

        INCLUDE("include-filter") {

            @Override
            public void addFilter(TypeFilter filter,
                    ClassPathScanningCandidateComponentProvider scanner) {

                scanner.addIncludeFilter(filter);
            }

        },
        EXCLUDE("exclude-filter") {

            @Override
            public void addFilter(TypeFilter filter,
                    ClassPathScanningCandidateComponentProvider scanner) {

                scanner.addExcludeFilter(filter);
            }
        };

        private String elementName;


        private Type(String elementName) {

            this.elementName = elementName;
        }


        /**
         * Returns the {@link Element} if the given {@link Node} is an
         * {@link Element} and it's name equals the one of the type.
         * 
         * @param node
         * @return
         */
        Element getElement(Node node) {

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String localName = node.getLocalName();
                if (elementName.equals(localName)) {
                    return (Element) node;
                }
            }

            return null;
        }


        abstract void addFilter(TypeFilter filter,
                ClassPathScanningCandidateComponentProvider scanner);
    }
}
