/*
 * Copyright 2011 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Abstraction of a {@link Property} of a domain class.
 *
 * @author Oliver Gierke
 */
public class Property {
	
	private static final Pattern SPLITTER = Pattern.compile("(?:_?(_*?[^_]+))");
	private static final String ERROR_TEMPLATE = "No property %s found for type %s";

	private final String name;
	private final TypeInformation<?> type;
	private final boolean isCollection;

	private Property next;


	/**
	 * Creates a leaf property (no nested ones) with the given name inside the
	 * given owning type.
	 *
	 * @param name
	 * @param owningType
	 */
	Property(String name, Class<?> owningType) {

		this(name, ClassTypeInformation.from(owningType));
	}

	Property(String name, TypeInformation<?> owningType) {

		Assert.hasText(name);
		Assert.notNull(owningType);

		String propertyName = StringUtils.uncapitalize(name);
		TypeInformation<?> type = owningType.getProperty(propertyName);

		if (type == null) {
			throw new IllegalArgumentException(String.format(ERROR_TEMPLATE,
					propertyName, owningType.getType()));
		}

		this.isCollection = type.isCollectionLike();
		this.type = getPropertyType(type);
		this.name = propertyName;
	}

	private TypeInformation<?> getPropertyType(TypeInformation<?> type) {

		if (type.isCollectionLike()) {
			return type.getComponentType();
		}

		if (type.isMap()) {
			return type.getMapValueType();
		}

		return type;
	}


	/**
	 * Creates a {@link Property} with the given name inside the given owning
	 * type and tries to resolve the other {@link String} to create nested
	 * properties.
	 *
	 * @param name
	 * @param owningType
	 * @param toTraverse
	 */
	Property(String name, TypeInformation<?> owningType, String toTraverse) {

		this(name, owningType);

		if (StringUtils.hasText(toTraverse)) {
			this.next = from(toTraverse, type);
		}
	}


	/**
	 * Returns the name of the {@link Property}.
	 *
	 * @return the name
	 */
	public String getName() {

		return name;
	}

	/**
	 * Returns the type of the property will return the plain resolved type for
	 * simple properties, the component type for any {@link Iterable} or the
	 * value type of a {@link java.util.Map} if the property is one.
	 *
	 * @return
	 */
	public Class<?> getType() {

		return this.type.getType();
	}


	/**
	 * Returns the next nested {@link Property}.
	 *
	 * @return the next nested {@link Property} or {@literal null} if no nested
	 *         {@link Property} available.
	 * @see #hasNext()
	 */
	public Property next() {

		return next;
	}


	/**
	 * Returns whether there is a nested {@link Property}. If this returns
	 * {@literal true} you can expect {@link #next()} to return a non-
	 * {@literal null} value.
	 *
	 * @return
	 */
	public boolean hasNext() {

		return next != null;
	}


	/**
	 * Returns the {@link Property} path in dot notation.
	 *
	 * @return
	 */
	public String toDotPath() {

		if (hasNext()) {
			return getName() + "." + next().toDotPath();
		}

		return getName();
	}


	/**
	 * Returns whether the {@link Property} is actually a collection.
	 *
	 * @return
	 */
	public boolean isCollection() {

		return isCollection;
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

		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}

		Property that = (Property) obj;

		return this.name.equals(that.name) && this.type.equals(type);
	}


	/*
			 * (non-Javadoc)
			 *
			 * @see java.lang.Object#hashCode()
			 */
	@Override
	public int hashCode() {

		return name.hashCode() + type.hashCode();
	}


	/**
	 * Extracts the {@link Property} chain from the given source {@link String}
	 * and type.
	 *
	 * @param source
	 * @param type
	 * @return
	 */
	public static Property from(String source, Class<?> type) {

		return from(source, ClassTypeInformation.from(type));
	}

	private static Property from(String source, TypeInformation<?> type) {
		
		List<String> iteratorSource = new ArrayList<String>();
		Matcher matcher = SPLITTER.matcher("_" + source);
		
		while (matcher.find()) {
			iteratorSource.add(matcher.group(1));
		}

		Iterator<String> parts = iteratorSource.iterator();

		Property result = null;
		Property current = null;

		while (parts.hasNext()) {
			if (result == null) {
				result = create(parts.next(), type);
				current = result;
			} else {
				current = create(parts.next(), current);
			}
		}

		return result;
	}


	/**
	 * Creates a new {@link Property} as subordinary of the given
	 * {@link Property}.
	 *
	 * @param source
	 * @param base
	 * @return
	 */
	private static Property create(String source, Property base) {

		Property property = create(source, base.type);
		base.next = property;
		return property;
	}


	/**
	 * Factory method to create a new {@link Property} for the given
	 * {@link String} and owning type. It will inspect the given source for
	 * camel-case parts and traverse the {@link String} along its parts starting
	 * with the entire one and chewing off parts from the right side then.
	 * Whenever a valid property for the given class is found, the tail will be
	 * traversed for subordinary properties of the just found one and so on.
	 *
	 * @param source
	 * @param type
	 * @return
	 */
	private static Property create(String source, TypeInformation<?> type) {

		return create(source, type, "");
	}


	/**
	 * Tries to look up a chain of {@link Property}s by trying the givne source
	 * first. If that fails it will split the source apart at camel case borders
	 * (starting from the right side) and try to look up a {@link Property} from
	 * the calculated head and recombined new tail and additional tail.
	 *
	 * @param source
	 * @param type
	 * @param addTail
	 * @return
	 */
	private static Property create(String source, TypeInformation<?> type, String addTail) {

		IllegalArgumentException exception = null;

		try {
			return new Property(source, type, addTail);
		} catch (IllegalArgumentException e) {
			exception = e;
		}

		Pattern pattern = Pattern.compile("[A-Z]?[a-z]*$");
		Matcher matcher = pattern.matcher(source);

		if (matcher.find() && matcher.start() != 0) {

			int position = matcher.start();
			String head = source.substring(0, position);
			String tail = source.substring(position);

			return create(head, type, tail + addTail);
		}

		throw exception;
	}
}
