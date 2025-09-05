/*
 * Copyright 2010-2025 the original author or authors.
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
package org.springframework.data.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parser to populate the given {@link ClassPathScanningCandidateComponentProvider} with {@link TypeFilter}s parsed from
 * the given {@link Element}'s children.
 *
 * @author Oliver Gierke
 */
public class TypeFilterParser {

	private static final String FILTER_TYPE_ATTRIBUTE = "type";
	private static final String FILTER_EXPRESSION_ATTRIBUTE = "expression";

	private final ReaderContext readerContext;
	private final ClassLoader classLoader;

	/**
	 * Creates a new {@link TypeFilterParser} with the given {@link ReaderContext}.
	 *
	 * @param readerContext must not be {@literal null}.
	 */
	public TypeFilterParser(XmlReaderContext readerContext) {
		this(readerContext, ConfigurationUtils.getRequiredClassLoader(readerContext));
	}

	/**
	 * Constructor to ease testing as {@link XmlReaderContext#getBeanClassLoader()} is final and thus cannot be mocked
	 * easily.
	 *
	 * @param readerContext must not be {@literal null}.
	 * @param classLoader must not be {@literal null}.
	 */
	TypeFilterParser(ReaderContext readerContext, ClassLoader classLoader) {

		Assert.notNull(readerContext, "ReaderContext must not be null");
		Assert.notNull(classLoader, "ClassLoader must not be null");

		this.readerContext = readerContext;
		this.classLoader = classLoader;
	}

	/**
	 * Returns all {@link TypeFilter} declared in nested elements of the given {@link Element}. Allows to selectively
	 * retrieve including or excluding filters based on the given {@link Type}.
	 *
	 * @param element must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public Collection<TypeFilter> parseTypeFilters(Element element, Type type) {

		NodeList nodeList = element.getChildNodes();
		Collection<TypeFilter> filters = new HashSet<>();

		for (int i = 0; i < nodeList.getLength(); i++) {

			Node node = nodeList.item(i);
			Element childElement = type.getElement(node);

			if (childElement == null) {
				continue;
			}

			try {
				filters.add(createTypeFilter(childElement, classLoader));
			} catch (RuntimeException e) {
				readerContext.error("Failed creating type filter: " + e.getMessage(), readerContext.extractSource(element),
						e.getCause());
			}
		}

		return filters;
	}

	/**
	 * Creates a {@link TypeFilter} instance from the given {@link Element} and {@link ClassLoader}.
	 *
	 * @param element must not be {@literal null}.
	 * @param classLoader must not be {@literal null}.
	 * @return
	 */
	protected TypeFilter createTypeFilter(Element element, ClassLoader classLoader) {

		String filterType = element.getAttribute(FILTER_TYPE_ATTRIBUTE);
		String expression = element.getAttribute(FILTER_EXPRESSION_ATTRIBUTE);

		try {

			FilterType filter = FilterType.fromString(filterType);
			return filter.getFilter(expression, classLoader);

		} catch (ClassNotFoundException ex) {
			throw new FatalBeanException("Type filter class not found: " + expression, ex);
		}
	}

	/**
	 * Enum representing all the filter types available for {@code include} and {@code exclude} elements. This acts as
	 * factory for {@link TypeFilter} instances.
	 *
	 * @author Oliver Gierke
	 * @see #getFilter(String, ClassLoader)
	 */
	private enum FilterType {

		ANNOTATION {
			@Override
			@SuppressWarnings("unchecked")
			public TypeFilter getFilter(String expression, ClassLoader classLoader) throws ClassNotFoundException {
				return new AnnotationTypeFilter((Class<Annotation>) classLoader.loadClass(expression));
			}
		},

		ASSIGNABLE {
			@Override
			public TypeFilter getFilter(String expression, ClassLoader classLoader) throws ClassNotFoundException {
				return new AssignableTypeFilter(classLoader.loadClass(expression));
			}
		},

		ASPECTJ {
			@Override
			public TypeFilter getFilter(String expression, ClassLoader classLoader) {
				return new AspectJTypeFilter(expression, classLoader);
			}
		},

		REGEX {
			@Override
			public TypeFilter getFilter(String expression, ClassLoader classLoader) {
				return new RegexPatternTypeFilter(Pattern.compile(expression));
			}
		},

		CUSTOM {
			@Override
			public TypeFilter getFilter(String expression, ClassLoader classLoader) throws ClassNotFoundException {

				Class<?> filterClass = classLoader.loadClass(expression);
				if (!TypeFilter.class.isAssignableFrom(filterClass)) {
					throw new IllegalArgumentException(
							"Class is not assignable to [" + TypeFilter.class.getName() + "]: " + expression);
				}
				return (TypeFilter) BeanUtils.instantiateClass(filterClass);
			}
		};

		/**
		 * Returns the {@link TypeFilter} for the given expression and {@link ClassLoader}.
		 *
		 * @param expression
		 * @param classLoader
		 * @return
		 * @throws ClassNotFoundException
		 */
		abstract TypeFilter getFilter(String expression, ClassLoader classLoader) throws ClassNotFoundException;

		/**
		 * Returns the {@link FilterType} for the given type as {@link String}.
		 *
		 * @param typeString
		 * @return
		 * @throws IllegalArgumentException if no {@link FilterType} could be found for the given argument.
		 */
		static FilterType fromString(String typeString) {

			for (FilterType filter : FilterType.values()) {
				if (filter.name().equalsIgnoreCase(typeString)) {
					return filter;
				}
			}

			throw new IllegalArgumentException("Unsupported filter type: " + typeString);
		}
	}

	public enum Type {

		INCLUDE("include-filter"), EXCLUDE("exclude-filter");

		private final String elementName;

		Type(String elementName) {
			this.elementName = elementName;
		}

		/**
		 * Returns the {@link Element} if the given {@link Node} is an {@link Element} and it's name equals the one of the
		 * type.
		 *
		 * @param node
		 * @return
		 */
		@Nullable
		Element getElement(Node node) {

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String localName = node.getLocalName();
				if (elementName.equals(localName)) {
					return (Element) node;
				}
			}

			return null;
		}
	}
}
