/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.mapping;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstraction of a {@link PropertyPath} of a domain class.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Mariusz Mączkowski
 * @author Johannes Englmeier
 */
public class PropertyPath implements Streamable<PropertyPath> {

	private static final String PARSE_DEPTH_EXCEEDED = "Trying to parse a path with depth greater than 1000; This has been disabled for security reasons to prevent parsing overflows";

	private static final String DELIMITERS = "_\\.";
	private static final Pattern SPLITTER = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", DELIMITERS));
	private static final Pattern SPLITTER_FOR_QUOTED = Pattern.compile("(?:[%s]?([%s]*?[^%s]+))".replaceAll("%s", "\\."));
	private static final Pattern NESTED_PROPERTY_PATTERN = Pattern.compile("\\p{Lu}[\\p{Ll}\\p{Nd}]*$");
	private static final Map<Property, PropertyPath> cache = new ConcurrentHashMap<>();

	private final TypeInformation<?> owningType;
	private final String name;
	private final TypeInformation<?> typeInformation;
	private final TypeInformation<?> actualTypeInformation;
	private final boolean isCollection;

	private @Nullable PropertyPath next;

	/**
	 * Creates a leaf {@link PropertyPath} (no nested ones) with the given name inside the given owning type.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @param owningType must not be {@literal null}.
	 */
	PropertyPath(String name, Class<?> owningType) {
		this(name, TypeInformation.of(owningType), Collections.emptyList());
	}

	/**
	 * Creates a leaf {@link PropertyPath} (no nested ones with the given name and owning type.
	 *
	 * @param name must not be {@literal null} or empty.
	 * @param owningType must not be {@literal null}.
	 * @param base the {@link PropertyPath} previously found.
	 */
	PropertyPath(String name, TypeInformation<?> owningType, List<PropertyPath> base) {

		Assert.hasText(name, "Name must not be null or empty");
		Assert.notNull(owningType, "Owning type must not be null");
		Assert.notNull(base, "Previously found properties must not be null");

		String decapitalized = Introspector.decapitalize(name);
		Property property = lookupProperty(owningType, decapitalized);

		if (property == null) {
			property = lookupProperty(owningType, StringUtils.uncapitalize(name));
		}

		if (property == null) {
			throw new PropertyReferenceException(decapitalized, owningType, base);
		}

		this.owningType = owningType;
		this.name = property.path;
		this.typeInformation = property.type;
		this.isCollection = this.typeInformation.isCollectionLike();
		this.actualTypeInformation = this.typeInformation.getActualType() == null ? this.typeInformation
				: this.typeInformation.getRequiredActualType();
	}

	@Nullable
	private static Property lookupProperty(TypeInformation<?> owningType, String name) {

		TypeInformation<?> propertyType = owningType.getProperty(name);

		return propertyType != null ? new Property(propertyType, name) : null;
	}

	/**
	 * Returns the owning type of the {@link PropertyPath}.
	 *
	 * @return the owningType will never be {@literal null}.
	 */
	public TypeInformation<?> getOwningType() {
		return owningType;
	}

	/**
	 * Returns the first part of the {@link PropertyPath}. For example:
	 *
	 * <pre class="code">
	 * PropertyPath.from("a.b.c", Some.class).getSegment();
	 * </pre>
	 *
	 * results in {@code a}.
	 *
	 * @return the name will never be {@literal null}.
	 */
	public String getSegment() {
		return name;
	}

	/**
	 * Returns the leaf property of the {@link PropertyPath}.
	 *
	 * @return will never be {@literal null}.
	 */
	public PropertyPath getLeafProperty() {

		PropertyPath result = this;

		while (result.hasNext()) {
			result = result.requiredNext();
		}

		return result;
	}

	/**
	 * Returns the type of the leaf property of the current {@link PropertyPath}.
	 *
	 * @return will never be {@literal null}.
	 */
	public Class<?> getLeafType() {
		return getLeafProperty().getType();
	}

	/**
	 * Returns the actual type of the property. Will return the plain resolved type for simple properties, the component
	 * type for any {@link Iterable} or the value type of a {@link java.util.Map}.
	 *
	 * @return the actual type of the property.
	 */
	public Class<?> getType() {
		return this.actualTypeInformation.getType();
	}

	public TypeInformation<?> getTypeInformation() {
		return this.typeInformation;
	}

	/**
	 * Returns the {@link PropertyPath} path that results from removing the first element of the current one. For example:
	 *
	 * <pre class="code">
	 * PropertyPath.from("a.b.c", Some.class).next().toDotPath();
	 * </pre>
	 *
	 * results in the output: {@code b.c}
	 *
	 * @return the next nested {@link PropertyPath} or {@literal null} if no nested {@link PropertyPath} available.
	 * @see #hasNext()
	 */
	@Nullable
	public PropertyPath next() {
		return next;
	}

	/**
	 * Returns whether there is a nested {@link PropertyPath}. If this returns {@literal true} you can expect
	 * {@link #next()} to return a non- {@literal null} value.
	 *
	 * @return
	 */
	public boolean hasNext() {
		return next != null;
	}

	/**
	 * Returns the {@link PropertyPath} in dot notation.
	 *
	 * @return the {@link PropertyPath} in dot notation.
	 */
	public String toDotPath() {

		if (hasNext()) {
			return getSegment() + "." + requiredNext().toDotPath();
		}

		return getSegment();
	}

	/**
	 * Returns whether the {@link PropertyPath} is actually a collection.
	 *
	 * @return {@literal true} whether the {@link PropertyPath} is actually a collection.
	 */
	public boolean isCollection() {
		return isCollection;
	}

	/**
	 * Returns the {@link PropertyPath} for the path nested under the current property.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	public PropertyPath nested(String path) {

		Assert.hasText(path, "Path must not be null or empty");

		String lookup = toDotPath().concat(".").concat(path);

		return PropertyPath.from(lookup, owningType);
	}

	/**
	 * Returns an {@link Iterator<PropertyPath>} that iterates over all the partial property paths with the same leaf type
	 * but decreasing length. For example:
	 *
	 * <pre class="code">
	 * PropertyPath propertyPath = PropertyPath.from("a.b.c", Some.class);
	 * propertyPath.forEach(p -> p.toDotPath());
	 * </pre>
	 *
	 * results in the dot paths: *
	 *
	 * <pre class="code">
	 * a.b.c
	 * b.c
	 * c
	 * </pre>
	 */
	@Override
	public Iterator<PropertyPath> iterator() {

		return new Iterator<PropertyPath>() {

			private @Nullable PropertyPath current = PropertyPath.this;

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			@Nullable
			public PropertyPath next() {

				PropertyPath result = current;

				if (result == null) {
					return null;
				}

				this.current = result.next();
				return result;
			}
		};
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof PropertyPath that)) {
			return false;
		}

		if (isCollection != that.isCollection) {
			return false;
		}

		return Objects.equals(this.owningType, that.owningType) && Objects.equals(this.name, that.name)
				&& Objects.equals(this.typeInformation, that.typeInformation)
				&& Objects.equals(this.actualTypeInformation, that.actualTypeInformation) && Objects.equals(next, that.next);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owningType, name, typeInformation, actualTypeInformation, isCollection, next);
	}

	/**
	 * Returns the next {@link PropertyPath}.
	 *
	 * @return the next {@link PropertyPath}.
	 * @throws IllegalStateException it there's no next one.
	 */
	private PropertyPath requiredNext() {

		PropertyPath result = next;

		if (result == null) {
			throw new IllegalStateException(
					"No next path available; Clients should call hasNext() before invoking this method");
		}

		return result;
	}

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and {@link TypeInformation}. <br />
	 * Uses {@link #SPLITTER} by default and {@link #SPLITTER_FOR_QUOTED} for {@link Pattern#quote(String) quoted}
	 * literals.
	 * <p>
	 * Separate parts of the path may be separated by {@code "."} or by {@code "_"} or by camel case. When the match to
	 * properties is ambiguous longer property names are preferred. So for "userAddressCity" the interpretation
	 * "userAddress.city" is preferred over "user.address.city".
	 * </p>
	 *
	 * @param source a String denoting the property path, must not be {@literal null}.
	 * @param type the owning type of the property path, must not be {@literal null}.
	 * @return a new {@link PropertyPath} guaranteed to be not {@literal null}.
	 */
	public static PropertyPath from(String source, Class<?> type) {
		return from(source, TypeInformation.of(type));
	}

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and {@link TypeInformation}. <br />
	 * Uses {@link #SPLITTER} by default and {@link #SPLITTER_FOR_QUOTED} for {@link Pattern#quote(String) quoted}
	 * literals.
	 * <p>
	 * Separate parts of the path may be separated by {@code "."} or by {@code "_"} or by camel case. When the match to
	 * properties is ambiguous longer property names are preferred. So for "userAddressCity" the interpretation
	 * "userAddress.city" is preferred over "user.address.city".
	 * </p>
	 *
	 * @param source a String denoting the property path, must not be {@literal null}.
	 * @param type the owning type of the property path, must not be {@literal null}.
	 * @return a new {@link PropertyPath} guaranteed to be not {@literal null}.
	 */
	public static PropertyPath from(String source, TypeInformation<?> type) {

		Assert.hasText(source, "Source must not be null or empty");
		Assert.notNull(type, "TypeInformation must not be null or empty");

		return cache.computeIfAbsent(new Property(type, source), it -> {

			List<String> iteratorSource = new ArrayList<>();

			Matcher matcher = isQuoted(it.path) ? SPLITTER_FOR_QUOTED.matcher(it.path.replace("\\Q", "").replace("\\E", ""))
					: SPLITTER.matcher("_" + it.path);

			while (matcher.find()) {
				iteratorSource.add(matcher.group(1));
			}

			Iterator<String> parts = iteratorSource.iterator();

			PropertyPath result = null;
			Stack<PropertyPath> current = new Stack<PropertyPath>();

			while (parts.hasNext()) {
				if (result == null) {
					result = create(parts.next(), it.type, current);
					current.push(result);
				} else {
					current.push(create(parts.next(), current));
				}
			}

			if (result == null) {
				throw new IllegalStateException(
						String.format("Expected parsing to yield a PropertyPath from %s but got null", source));
			}

			return result;
		});
	}

	private static boolean isQuoted(String source) {
		return source.matches("^\\\\Q.*\\\\E$");
	}

	/**
	 * Creates a new {@link PropertyPath} as subordinary of the given {@link PropertyPath}.
	 *
	 * @param source
	 * @param base
	 * @return
	 */
	private static PropertyPath create(String source, Stack<PropertyPath> base) {

		PropertyPath previous = base.peek();

		PropertyPath propertyPath = create(source, previous.typeInformation.getRequiredActualType(), base);
		previous.next = propertyPath;
		return propertyPath;
	}

	/**
	 * Factory method to create a new {@link PropertyPath} for the given {@link String} and owning type. It will inspect
	 * the given source for camel-case parts and traverse the {@link String} along its parts starting with the entire one
	 * and chewing off parts from the right side then. Whenever a valid property for the given class is found, the tail
	 * will be traversed for subordinary properties of the just found one and so on.
	 *
	 * @param source
	 * @param type
	 * @return
	 */
	private static PropertyPath create(String source, TypeInformation<?> type, List<PropertyPath> base) {
		return create(source, type, "", base);
	}

	/**
	 * Tries to look up a chain of {@link PropertyPath}s by trying the given source first. If that fails it will split the
	 * source apart at camel case borders (starting from the right side) and try to look up a {@link PropertyPath} from
	 * the calculated head and recombined new tail and additional tail.
	 *
	 * @param source
	 * @param type
	 * @param addTail
	 * @return
	 */
	private static PropertyPath create(String source, TypeInformation<?> type, String addTail, List<PropertyPath> base) {

		if (base.size() > 1000) {
			throw new IllegalArgumentException(PARSE_DEPTH_EXCEEDED);
		}

		PropertyReferenceException exception = null;
		PropertyPath current = null;

		try {

			current = new PropertyPath(source, type, base);

			if (!base.isEmpty()) {
				base.get(base.size() - 1).next = current;
			}

			List<PropertyPath> newBase = new ArrayList<>(base);
			newBase.add(current);

			if (StringUtils.hasText(addTail)) {
				current.next = create(addTail, current.actualTypeInformation, newBase);
			}

			return current;

		} catch (PropertyReferenceException e) {

			if (current != null) {
				throw e;
			}

			exception = e;
		}

		Matcher matcher = NESTED_PROPERTY_PATTERN.matcher(source);

		if (matcher.find() && matcher.start() != 0) {

			int position = matcher.start();
			String head = source.substring(0, position);
			String tail = source.substring(position);

			try {
				return create(head, type, tail + addTail, base);
			} catch (PropertyReferenceException e) {
				throw e.hasDeeperResolutionDepthThan(exception) ? e : exception;
			}
		}

		throw exception;
	}

	@Override
	public String toString() {
		return String.format("%s.%s", owningType.getType().getSimpleName(), toDotPath());
	}

	private static final class Property {

		private final TypeInformation<?> type;
		private final String path;

		private Property(TypeInformation<?> type, String path) {
			this.type = type;
			this.path = path;
		}

		@Override
		public boolean equals(@Nullable Object obj) {

			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Property that)) {
				return false;
			}

			return Objects.equals(this.type, that.type) &&
					Objects.equals(this.path, that.path);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, path);
		}

		@Override
		public String toString() {

			return "Key[" +
					"type=" + type + ", " +
					"path=" + path + ']';
		}
	}
}
