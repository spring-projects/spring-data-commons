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

import org.jspecify.annotations.Nullable;

/**
 * Interface providing type-safe property references.
 * <p>
 * This functional interface is typically implemented through method references that allow for compile-time type safety
 * and refactoring support. Instead of string-based property names that are easy to miss when changing the domain model,
 * {@code PropertyReference} leverages Java's declarative method references to ensure type-safe property access.
 * <p>
 * Create a typed property reference using the static factory method {@link #property(PropertyReference)} with a method
 * reference, for example:
 *
 * <pre class="code">
 * PropertyReference.property(Person::getName);
 * </pre>
 *
 * The resulting object can be used to obtain the {@link #getName() property name} and to interact with the target
 * property. Typed references can be used to compose {@link TypedPropertyPath property paths} to navigate nested object
 * structures using {@link #then(PropertyReference)}:
 *
 * <pre class="code">
 * TypedPropertyPath&lt;Person, String&gt; city = PropertyReference.of(Person::getAddress).then(Address::getCity);
 * </pre>
 * <p>
 * The generic type parameters preserve type information across the property path chain: {@code T} represents the owning
 * type of the current segment (or the root type for composed paths), while {@code P} represents the property value type
 * at this segment. Composition automatically flows type information forward, ensuring that {@code then()} preserves the
 * full chain's type safety.
 * <p>
 * Implement {@code PropertyReference} using method references (strongly recommended) or lambdas that directly access a
 * property getter. Constructor references, method calls with parameters, and complex expressions are not supported and
 * result in {@link org.springframework.dao.InvalidDataAccessApiUsageException}. Unlike method references, introspection
 * of lambda expressions requires bytecode analysis of the declaration site classes and thus depends on their
 * availability at runtime.
 *
 * @param <T> the owning type of this property.
 * @param <P> the property value type.
 * @author Mark Paluch
 * @since 4.1
 * @see #property(PropertyReference)
 * @see #then(PropertyReference)
 * @see #of(PropertyReference)
 * @see #ofMany(PropertyReference)
 * @see TypedPropertyPath
 * @see java.beans.PropertyDescriptor
 */
@FunctionalInterface
public interface PropertyReference<T, P extends @Nullable Object> extends Serializable {

	/**
	 * Syntax sugar to create a {@link PropertyReference} from a method reference to a Java beans property. Suitable for
	 * static imports.
	 * <p>
	 * This method returns a resolved {@link PropertyReference} by introspecting the given method reference.
	 *
	 * @param property the method reference to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property reference.
	 */
	static <T, P extends @Nullable Object> PropertyReference<T, P> property(PropertyReference<T, P> property) {
		return of(property);
	}

	/**
	 * Syntax sugar to create a {@link PropertyReference} from a method reference to a Java beans property.
	 * <p>
	 * This method returns a resolved {@link PropertyReference} by introspecting the given method reference.
	 *
	 * @param property the method reference to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property reference.
	 */
	static <T, P extends @Nullable Object> PropertyReference<T, P> of(PropertyReference<T, P> property) {
		return PropertyReferences.of(property);
	}

	/**
	 * Syntax sugar to create a {@link PropertyReference} from a method reference to a Java beans property.
	 * <p>
	 * This method returns a resolved {@link PropertyReference} by introspecting the given method reference. Note that
	 * {@link #get(Object)} becomes unusable for collection properties as the property type adapted from
	 * {@code Iterable &lt;P&gt;} and a single {@code P} cannot represent a collection of items.
	 *
	 * @param property the method reference to a Java beans property.
	 * @param <T> owning type.
	 * @param <P> property type.
	 * @return the typed property reference.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T, P> PropertyReference<T, P> ofMany(PropertyReference<T, ? extends Iterable<P>> property) {
		return (PropertyReference) PropertyReferences.of(property);
	}

	/**
	 * Get the property value for the given object.
	 *
	 * @param obj the object to get the property value from.
	 * @return the property value.
	 */
	@Nullable
	P get(T obj);

	/**
	 * Returns the owning type of the referenced property.
	 *
	 * @return the owningType will never be {@literal null}.
	 */
	default TypeInformation<T> getOwningType() {
		return PropertyReferences.of(this).getOwningType();
	}

	/**
	 * Returns the name of the property.
	 *
	 * @return the current property name.
	 */
	default String getName() {
		return PropertyReferences.of(this).getName();
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
	default TypeInformation<?> getTypeInformation() {
		return PropertyReferences.of(this).getTypeInformation();
	}

	/**
	 * Returns whether the property is a collection.
	 *
	 * @return {@literal true} if the property is a collection.
	 * @see #getTypeInformation()
	 * @see TypeInformation#isCollectionLike()
	 */
	default boolean isCollection() {
		return getTypeInformation().isCollectionLike();
	}

	/**
	 * Extend the property to a property path by appending the {@code next} path segment and return a new property path
	 * instance.
	 *
	 * @param next the next property path segment as method reference accepting the owner object {@code P} type and
	 *          returning {@code N} as result of accessing the property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	default <N extends @Nullable Object> TypedPropertyPath<T, N> then(PropertyReference<P, N> next) {
		return TypedPropertyPaths.compose(this, next);
	}

	/**
	 * Extend the property to a property path by appending the {@code next} path segment and return a new property path
	 * instance.
	 * <p>
	 * Note that {@link #get(Object)} becomes unusable for collection properties as the property type adapted from
	 * {@code Iterable &lt;P&gt;} and a single {@code P} cannot represent a collection of items.
	 *
	 * @param next the next property path segment as method reference accepting the owner object {@code P} type and
	 *          returning {@code N} as result of accessing the property.
	 * @param <N> the new property value type.
	 * @return a new composed {@code TypedPropertyPath}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	default <N extends @Nullable Object> TypedPropertyPath<T, N> thenMany(
			PropertyReference<P, ? extends Iterable<N>> next) {
		return (TypedPropertyPath) TypedPropertyPaths.compose(this, next);
	}

}
