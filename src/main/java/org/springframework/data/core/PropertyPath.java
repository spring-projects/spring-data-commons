/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.core;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Abstraction of a {@link PropertyPath} within a domain class.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Mariusz Mączkowski
 * @author Johannes Englmeier
 */
public interface PropertyPath extends Streamable<PropertyPath> {

	/**
	 * Returns the owning type of the {@link PropertyPath}.
	 *
	 * @return the owningType will never be {@literal null}.
	 */
	TypeInformation<?> getOwningType();

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
	String getSegment();

	/**
	 * Returns the leaf property of the {@link PropertyPath}.
	 *
	 * @return will never be {@literal null}.
	 */
	default PropertyPath getLeafProperty() {

		PropertyPath result = this;

		while (result != null && result.hasNext()) {
			result = result.next();
		}

		return result == null ? this : result;
	}

	/**
	 * Returns the type of the leaf property of the current {@link PropertyPath}.
	 *
	 * @return will never be {@literal null}.
	 */
	default Class<?> getLeafType() {
		return getLeafProperty().getType();
	}

	/**
	 * Returns the actual type of the property. Will return the plain resolved type for simple properties, the component
	 * type for any {@link Iterable} or the value type of a {@link java.util.Map}.
	 *
	 * @return the actual type of the property.
	 */
	default Class<?> getType() {
		return getTypeInformation().getRequiredActualType().getType();
	}

	/**
	 * Returns the type information of the property.
	 *
	 * @return the actual type of the property.
	 */
	TypeInformation<?> getTypeInformation();

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
	PropertyPath next();

	/**
	 * Returns whether there is a nested {@link PropertyPath}. If this returns {@literal true} you can expect
	 * {@link #next()} to return a non- {@literal null} value.
	 *
	 * @return
	 */
	default boolean hasNext() {
		return next() != null;
	}

	/**
	 * Returns the {@link PropertyPath} in dot notation.
	 *
	 * @return the {@link PropertyPath} in dot notation.
	 */
	default String toDotPath() {

		PropertyPath next = next();
		return next != null ? getSegment() + "." + next.toDotPath() : getSegment();
	}

	/**
	 * Returns whether the {@link PropertyPath} is actually a collection.
	 *
	 * @return {@literal true} whether the {@link PropertyPath} is actually a collection.
	 */
	default boolean isCollection() {
		return getTypeInformation().isCollectionLike();
	}

	/**
	 * Returns the {@link PropertyPath} for the path nested under the current property.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @return will never be {@literal null}.
	 */
	default PropertyPath nested(String path) {

		Assert.hasText(path, "Path must not be null or empty");

		String lookup = toDotPath().concat(".").concat(path);

		return SimplePropertyPath.from(lookup, getOwningType());
	}

	/**
	 * Returns an {@link Iterator Iterator of PropertyPath} that iterates over all the partial property paths with the
	 * same leaf type but decreasing length. For example:
	 *
	 * <pre class="code">
	 * PropertyPath propertyPath = PropertyPath.from("a.b.c", Some.class);
	 * propertyPath.forEach(p -> p.toDotPath());
	 * </pre>
	 *
	 * results in the dot paths:
	 *
	 * <pre class="code">
	 * a.b.c
	 * b.c
	 * c
	 * </pre>
	 */
	@Override
	Iterator<PropertyPath> iterator();

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and {@link TypeInformation}. <br />
	 * Uses {@code (?:[%s]?([%s]*?[^%s]+))} by default and {@code (?:[%s]?([%s]*?[^%s]+))} for
	 * {@link Pattern#quote(String) quoted} literals.
	 * <p>
	 * Separate parts of the path may be separated by {@code "."} or by {@code "_"} or by camel case. When the match to
	 * properties is ambiguous longer property names are preferred. So for {@code userAddressCity} the interpretation
	 * {@code userAddress.city} is preferred over {@code user.address.city}.
	 *
	 * @param source a String denoting the property path, must not be {@literal null}.
	 * @param type the owning type of the property path, must not be {@literal null}.
	 * @return a new {@link PropertyPath} guaranteed to be not {@literal null}.
	 */
	static PropertyPath from(String source, Class<?> type) {
		return from(source, TypeInformation.of(type));
	}

	/**
	 * Extracts the {@link PropertyPath} chain from the given source {@link String} and {@link TypeInformation}. <br />
	 * Uses {@code (?:[%s]?([%s]*?[^%s]+))} by default and {@code (?:[%s]?([%s]*?[^%s]+))} for
	 * {@link Pattern#quote(String) quoted} literals.
	 * <p>
	 * Separate parts of the path may be separated by {@code "."} or by {@code "_"} or by camel case. When the match to
	 * properties is ambiguous longer property names are preferred. So for {@code userAddressCity} the interpretation
	 * {@code userAddress.city} is preferred over {@code user.address.city}.
	 *
	 * @param source a String denoting the property path, must not be {@literal null}.
	 * @param type the owning type of the property path, must not be {@literal null}.
	 * @return a new {@link PropertyPath} guaranteed to be not {@literal null}.
	 */
	static PropertyPath from(String source, TypeInformation<?> type) {
		return SimplePropertyPath.from(source, type);
	}

}
