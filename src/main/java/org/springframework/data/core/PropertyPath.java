/*
 * Copyright 2011-present the original author or authors.
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
 * <p>
 * Property paths allow to navigate nested properties such as {@code address.city.name} and provide metadata for each
 * segment of the path. Paths are represented in dot-path notation and are resolved from an owning type, for example:
 *
 * <pre class="code">
 * PropertyPath.from("address.city.name", Person.class);
 * </pre>
 *
 * Paths are cached on a best-effort basis using a weak reference cache to avoid repeated introspection if GC pressure
 * permits.
 * <p>
 * A typed variant of {@link PropertyPath} is available as {@link TypedPropertyPath} through
 * {@link #of(PropertyReference)} to leverage method references for a type-safe usage across application code.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Mariusz Mączkowski
 * @author Johannes Englmeier
 * @see PropertyReference
 * @see TypedPropertyPath
 * @see java.beans.PropertyDescriptor
 */
public interface PropertyPath extends Streamable<PropertyPath> {

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference to a Java beans property.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference.
	 *
	 * @param property the method reference referring to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 * @since 4.1
	 */
	static <T, P> TypedPropertyPath<T, P> of(PropertyReference<T, P> property) {
		return TypedPropertyPaths.of(property);
	}

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference to a Java beans collection property.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference.
	 *
	 * @param property the method reference referring to a property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 * @since 4.1
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T, P> TypedPropertyPath<T, P> ofMany(PropertyReference<T, ? extends Iterable<P>> property) {
		return (TypedPropertyPath) TypedPropertyPaths.of(property);
	}

	/**
	 * Returns the owning type of the {@link PropertyPath}.
	 *
	 * @return the owningType will never be {@literal null}.
	 */
	TypeInformation<?> getOwningType();

	/**
	 * Returns the current property path segment (i.e. first part of {@link #toDotPath()}).
	 * <p>
	 * For example:
	 *
	 * <pre class="code">
	 * PropertyPath.from("address.city.name", Person.class).getSegment();
	 * </pre>
	 *
	 * results in {@code address}.
	 *
	 * @return the current property path segment.
	 */
	String getSegment();

	/**
	 * Returns the leaf property of the {@link PropertyPath}. Either this property if the path ends here or the last
	 * property in the chain.
	 *
	 * @return leaf property.
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
	 * @see #getLeafProperty()
	 */
	default Class<?> getLeafType() {
		return getLeafProperty().getType();
	}

	/**
	 * Returns the actual type of the property at this segment. Will return the plain resolved type for simple properties,
	 * the component type for any {@link Iterable} or the value type of {@link java.util.Map} properties.
	 *
	 * @return the actual type of the property.
	 * @see #getTypeInformation()
	 * @see TypeInformation#getRequiredActualType()
	 */
	default Class<?> getType() {
		return getTypeInformation().getRequiredActualType().getType();
	}

	/**
	 * Returns the type information for the property at this segment.
	 *
	 * @return the type information for the property at this segment.
	 */
	TypeInformation<?> getTypeInformation();

	/**
	 * Returns whether the current property path segment is a collection.
	 *
	 * @return {@literal true} if the current property path segment is a collection.
	 * @see #getTypeInformation()
	 * @see TypeInformation#isCollectionLike()
	 */
	default boolean isCollection() {
		return getTypeInformation().isCollectionLike();
	}

	/**
	 * Returns the next {@code PropertyPath} segment in the property path chain.
	 *
	 * <pre class="code">
	 * PropertyPath.from("address.city.name", Person.class).next().toDotPath();
	 * </pre>
	 *
	 * results in the output: {@code city.name}.
	 *
	 * @return the next {@code PropertyPath} or {@literal null} if the path does not contain further segments.
	 * @see #hasNext()
	 */
	@Nullable
	PropertyPath next();

	/**
	 * Returns {@literal true} if the property path contains further segments or {@literal false} if the path ends at this
	 * segment.
	 *
	 * @return {@literal true} if the property path contains further segments or {@literal false} if the path ends at this
	 *         segment.
	 */
	default boolean hasNext() {
		return next() != null;
	}

	/**
	 * Returns the {@code PropertyPath} in dot notation.
	 *
	 * @return the {@code PropertyPath} in dot notation.
	 */
	default String toDotPath() {

		PropertyPath next = next();
		return next != null ? getSegment() + "." + next.toDotPath() : getSegment();
	}

	/**
	 * Returns the {@code PropertyPath} for the path nested under the current property.
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
	 * Returns an {@link Iterator Iterator of PropertyPath} that iterates over all property path segments. For example:
	 *
	 * <pre class="code">
	 * PropertyPath path = PropertyPath.from("address.city.name", Person.class);
	 * path.forEach(p -> p.toDotPath());
	 * </pre>
	 *
	 * results in the dot paths:
	 *
	 * <pre class="code">
	 * address.city.name     (this object)
	 * city.name             (next() object)
	 * name             (next().next() object)
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
