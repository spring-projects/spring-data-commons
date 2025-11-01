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
 * Type-safe representation of a property path using getter method references or lambda expressions.
 * <p>
 * This functional interface extends {@link PropertyPath} to provide compile-time type safety and refactoring support.
 * Instead of using {@link PropertyPath#from(String, TypeInformation) string-based property paths} for textual property
 * representation that are easy to miss when changing the domain model, {@code TypedPropertyPath} leverages Java's
 * declarative method references and lambda expressions to ensure type-safe property access.
 * <p>
 * Typed property paths can be created directly they are accepted used or conveniently using the static factory method
 * {@link #of(TypedPropertyPath)} with method references:
 *
 * <pre class="code">
 * PropertyPath.of(Person::getName);
 * </pre>
 *
 * Property paths can be composed to navigate nested properties using {@link #then(TypedPropertyPath)}:
 *
 * <pre class="code">
 * PropertyPath.of(Person::getAddress).then(Address::getCity);
 * </pre>
 * <p>
 * The interface maintains type information throughout the property path chain: the {@code T} type parameter represents
 * its owning type (root type for composed paths), while {@code P} represents the property value type at this path
 * segment.
 * <p>
 * Use method references (recommended) or lambdas that access a property getter to implement {@code TypedPropertyPath}.
 * Usage of constructor references, method calls with parameters, and complex expressions results in
 * {@link org.springframework.dao.InvalidDataAccessApiUsageException}. In contrast to method references, introspection
 * of lambda expressions requires bytecode analysis of the declaration site classes and therefore presence of their
 * class files.
 *
 * @param <T> the owning type of the property path segment, root type for composed paths.
 * @param <P> the property type at this path segment.
 * @author Mark Paluch
 * @since 4.1
 * @see PropertyPath#of(TypedPropertyPath)
 * @see #then(TypedPropertyPath)
 */
@FunctionalInterface
public interface TypedPropertyPath<T, P> extends PropertyPath, Serializable {

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

}
