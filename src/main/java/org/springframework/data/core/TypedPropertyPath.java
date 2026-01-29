/*
 * Copyright 2025-present the original author or authors.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;

/**
 * Interface providing type-safe property path navigation through method references expressions.
 * <p>
 * This functional interface extends {@link PropertyPath} to provide compile-time type safety and refactoring support.
 * Instead of using {@link PropertyPath#from(String, TypeInformation) string-based property paths} for textual property
 * representation that are easy to miss when changing the domain model, {@code TypedPropertyPath} leverages Java's
 * declarative method references to ensure type-safe property access.
 * <p>
 * Create a typed property path using the static factory method {@link #of(TypedPropertyPath)} with a method reference ,
 * for example:
 *
 * <pre class="code">
 * TypedPropertyPath.path(Person::getName);
 * </pre>
 *
 * The resulting object can be used to obtain the {@link #toDotPath() dot-path} and to interact with the targeting
 * property. Typed paths allow for composition to navigate nested object structures using
 * {@link #then(PropertyReference)}:
 *
 * <pre class="code">
 * // factory method chaining
 * TypedPropertyPath&lt;Person, String&gt; city = TypedPropertyPath.path(Person::getAddress, Address::getCity);
 *
 * // fluent API
 * TypedPropertyPath&lt;Person, String&gt; city = TypedPropertyPath.of(Person::getAddress).then(Address::getCity);
 * </pre>
 * <p>
 * The generic type parameters preserve type information across the property path chain: {@code T} represents the owning
 * type of the current segment (or the root type for composed paths), while {@code P} represents the property value type
 * at this segment. Composition automatically flows type information forward, ensuring that {@code then()} preserves the
 * full chain's type safety.
 * <p>
 * Implement {@code TypedPropertyPath} using method references (strongly recommended)s that directly access a property
 * getter. Constructor references, method calls with parameters, and complex expressions are not supported and result in
 * {@link org.springframework.dao.InvalidDataAccessApiUsageException}. Unlike method references, introspection of lambda
 * expressions requires bytecode analysis of the declaration site classes and thus depends on their availability at
 * runtime.
 *
 * @param <T> the owning type of this path segment; the root type for composed paths.
 * @param <P> the property value type at this path segment.
 * @author Mark Paluch
 * @since 4.1
 * @see #path(PropertyReference)
 * @see #of(PropertyReference)
 * @see #ofMany(PropertyReference)
 * @see #then(PropertyReference)
 * @see PropertyReference
 * @see PropertyPath#of(PropertyReference)
 */
@FunctionalInterface
public interface TypedPropertyPath<T, P extends @Nullable Object> extends PropertyPath, Serializable {

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a property described as method reference to a Java beans
	 * property. Suitable for static imports.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference.
	 *
	 * @param property the method reference to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 */
	static <T, P extends @Nullable Object> TypedPropertyPath<T, P> path(PropertyReference<T, P> property) {
		return TypedPropertyPaths.of(property);
	}

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference to a Java beans property.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference.
	 *
	 * @param property the method reference to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 */
	static <T, P extends @Nullable Object> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> property) {
		return TypedPropertyPaths.of(property);
	}

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference to a Java beans collection property.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference.
	 * <p>
	 * Note that {@link #get(Object)} becomes unusable for collection properties as the property type adapted from
	 * {@code Iterable &lt;P&gt;} and a single {@code P} cannot represent a collection of items.
	 *
	 * @param property the method reference to a Java beans collection property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T, P> TypedPropertyPath<T, P> ofMany(TypedPropertyPath<T, ? extends Iterable<P>> property) {
		return (TypedPropertyPath) TypedPropertyPaths.of(property);
	}

	/**
	 * Get the property value for the given object.
	 *
	 * @param obj the object to get the property value from.
	 * @return the property value.
	 */
	@Nullable
	P get(T obj);

	@Override
	default TypeInformation<T> getOwningType() {
		return TypedPropertyPaths.of(this).getOwningType();
	}

	@Override
	default String getSegment() {
		return TypedPropertyPaths.of(this).getSegment();
	}

	@Override
	default TypeInformation<P> getTypeInformation() {
		return TypedPropertyPaths.of(this).getTypeInformation();
	}

	@Override
	@Nullable
	default PropertyPath next() {
		return TypedPropertyPath.of(this).next();
	}

	@Override
	default boolean hasNext() {
		return TypedPropertyPath.of(this).hasNext();
	}

	@Override
	default Iterator<PropertyPath> iterator() {
		return Collections.singletonList((PropertyPath) this).iterator();
	}

	/**
	 * Extend the property path by appending the {@code next} path segment and return a new property path instance.
	 *
	 * @param next the next property path segment as method reference accepting the owner object {@code P} type and
	 *          returning {@code N} as result of accessing a property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	default <N extends @Nullable Object> TypedPropertyPath<T, N> then(PropertyReference<P, N> next) {
		return TypedPropertyPaths.compose(this, next);
	}

	/**
	 * Extend the property path by appending the {@code next} path segment and return a new property path instance.
	 * <p>
	 * Note that {@link #get(Object)} becomes unusable for collection properties as the property type adapted from
	 * {@code Iterable &lt;P&gt;} and a single {@code P} cannot represent a collection of items.
	 *
	 * @param next the next property path segment as method reference accepting the owner object {@code P} type and
	 *          returning {@code N} as result of accessing a property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	default <N extends @Nullable Object> TypedPropertyPath<T, N> thenMany(
			PropertyReference<P, ? extends Iterable<N>> next) {
		return (TypedPropertyPath) TypedPropertyPaths.compose(this, PropertyReference.of(next));
	}

}
