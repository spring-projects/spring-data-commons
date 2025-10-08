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
package org.springframework.data.mapping;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.springframework.data.util.TypeInformation;

/**
 * Type-safe representation of a property path expressed through method references.
 * <p>
 * This functional interface extends {@link PropertyPath} to provide compile-time type safety when declaring property
 * paths. Instead of using {@link PropertyPath#from(String, TypeInformation) string-based property paths} that represent
 * references to properties textually and that are prone to refactoring issues, {@code TypedPropertyPath} leverages
 * Java's declarative method references and lambda expressions to ensure type-safe property access.
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
 * PropertyPath.of(Person::getAddress).then(Address::getCountry).then(Country::getName);
 * </pre>
 * <p>
 * The interface maintains type information throughout the property path chain: the {@code T} type parameter represents
 * its owning type (root type for composed paths), while {@code P} represents the property value type at this path
 * segment.
 * <p>
 * As a functional interface, {@code TypedPropertyPath} should be implemented as method reference (recommended).
 * Alternatively, the interface can be implemented as lambda extracting a property value from an object of type
 * {@code T}. Implementations must ensure that the method reference or lambda correctly represents a property access
 * through a method invocation or by field access. Arbitrary calls to non-getter methods (i.e. methods accepting
 * parameters or arbitrary method calls on types other than the owning type are not allowed and will fail with
 * {@link org.springframework.dao.InvalidDataAccessApiUsageException}.
 * <p>
 * Note that using lambda expressions requires bytecode analysis of the declaration site classes and therefore presence
 * of their class files.
 * 
 * @param <T> the owning type of the property path segment, but typically the root type for composed property paths.
 * @param <P> the property value type at this path segment.
 * @author Mark Paluch
 * @see PropertyPath
 * @see #of(TypedPropertyPath)
 * @see #then(TypedPropertyPath)
 */
@FunctionalInterface
public interface TypedPropertyPath<T, P> extends PropertyPath, Serializable {

	/**
	 * Syntax sugar to create a {@link TypedPropertyPath} from a method reference or lambda.
	 * <p>
	 * This method returns a resolved {@link TypedPropertyPath} by introspecting the given lambda.
	 *
	 * @param lambda the method reference or lambda.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property path.
	 */
	static <T, P> TypedPropertyPath<T, P> of(TypedPropertyPath<T, P> lambda) {
		return TypedPropertyPaths.of(lambda);
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
		return TypedPropertyPaths.getPropertyPathInformation(this).owner();
	}

	@Override
	default String getSegment() {
		return TypedPropertyPaths.getPropertyPathInformation(this).property().getName();
	}

	@Override
	default TypeInformation<?> getTypeInformation() {
		return TypedPropertyPaths.getPropertyPathInformation(this).propertyType();
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
	 * Extend the property path by appending the {@code next} path segment and returning a new property path instance..
	 *
	 * @param next the next property path segment accepting a property path owned by the {@code P} type.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	default <N> TypedPropertyPath<T, N> then(TypedPropertyPath<P, N> next) {
		return TypedPropertyPaths.compose(this, of(next));
	}
}
