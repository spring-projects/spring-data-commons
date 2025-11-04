/*
 * Copyright 2025 the original author or authors.
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
 * Interface providing type-safe property path navigation through method references or lambda expressions.
 * <p>
 * This functional interface extends {@link PropertyPath} to provide compile-time type safety and refactoring support.
 * Instead of using {@link PropertyPath#from(String, TypeInformation) string-based property paths} for textual property
 * representation that are easy to miss when changing the domain model, {@code TypedPropertyPath} leverages Java's
 * declarative method references and lambda expressions to ensure type-safe property access.
 * <p>
 * Create a typed property path using the static factory method {@link #of(TypedPropertyPath)} with a method reference
 * or lambda, for example:
 *
 * <pre class="code">
 * TypedPropertyPath&lt;Person, String&gt; name = TypedPropertyPath.of(Person::getName);
 * </pre>
 *
 * The resulting object can be used to obtain the {@link #toDotPath() dot-path} and to interact with the targetting
 * property. Typed paths allow for composition to navigate nested object structures using
 * {@link #then(TypedPropertyPath)}:
 *
 * <pre class="code">
 * TypedPropertyPath&lt;Person, String&gt; city = TypedPropertyPath.of(Person::getAddress).then(Address::getCity);
 * </pre>
 * <p>
 * The generic type parameters preserve type information across the property path chain: {@code T} represents the owning
 * type of the current segment (or the root type for composed paths), while {@code P} represents the property value type
 * at this segment. Composition automatically flows type information forward, ensuring that {@code then()} preserves the
 * full chain's type safety.
 * <p>
 * Implement {@code TypedPropertyPath} using method references (strongly recommended) or lambdas that directly access a
 * property getter. Constructor references, method calls with parameters, and complex expressions are not supported and
 * result in {@link org.springframework.dao.InvalidDataAccessApiUsageException}. Unlike method references, introspection
 * of lambda expressions requires bytecode analysis of the declaration site classes and thus depends on their
 * availability at runtime.
 *
 * @param <T> the owning type of this path segment; the root type for composed paths.
 * @param <P> the property value type at this path segment.
 * @author Mark Paluch
 * @since 4.1
 * @see PropertyPath#of(TypedPropertyPath)
 * @see #then(TypedPropertyPath)
 */
@FunctionalInterface
public interface TypedPropertyPath<T, P extends @Nullable Object> extends PropertyPath, Serializable {

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference or lambda.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference or lambda.
	 *
	 * @param propertyPath the method reference or lambda.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 */
	static <T, P extends @Nullable Object> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> propertyPath) {
		return TypedPropertyPaths.of(propertyPath);
	}

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference or lambda for a collection property.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given method reference or lambda.
	 *
	 * @param propertyPath the method reference or lambda.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 * @since 4.1
	 */
	static <T, P> TypedPropertyPath<T, P> ofMany(TypedPropertyPath<T, ? extends Iterable<P>> propertyPath) {
		return (TypedPropertyPath) TypedPropertyPaths.of(propertyPath);
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
	default TypeInformation<?> getOwningType() {
		return TypedPropertyPaths.getMetadata(this).owner();
	}

	@Override
	default String getSegment() {
		return TypedPropertyPaths.getMetadata(this).property();
	}

	@Override
	default TypeInformation<?> getTypeInformation() {
		return TypedPropertyPaths.getMetadata(this).propertyType();
	}

	@Override
	@Nullable
	default PropertyPath next() {
		return null;
	}

	@Override
	default boolean hasNext() {
		return false;
	}

	@Override
	default Iterator<PropertyPath> iterator() {
		return Collections.singletonList((PropertyPath) this).iterator();
	}

	/**
	 * Extend the property path by appending the {@code next} path segment and returning a new property path instance.
	 *
	 * @param next the next property path segment as method reference or lambda accepting the owner object {@code P} type
	 *          and returning {@code N} as result of accessing a property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	default <N extends @Nullable Object> TypedPropertyPath<T, N> then(TypedPropertyPath<P, N> next) {
		return TypedPropertyPaths.compose(this, of(next));
	}

	/**
	 * Extend the property path by appending the {@code next} path segment and returning a new property path instance.
	 *
	 * @param next the next property path segment as method reference or lambda accepting the owner object {@code P} type
	 *          and returning {@code N} as result of accessing a property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	default <N extends @Nullable Object> TypedPropertyPath<T, N> thenMany(
			TypedPropertyPath<P, ? extends Iterable<N>> next) {
		return (TypedPropertyPath) TypedPropertyPaths.compose(this, of(next));
	}

}
