/*
 * Copyright 2011-2021 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
public interface PersistentProperty<P extends PersistentProperty<P>> {

	/**
	 * Returns the {@link PersistentEntity} owning the current {@link PersistentProperty}.
	 *
	 * @return never {@literal null}.
	 */
	PersistentEntity<?, P> getOwner();

	/**
	 * The name of the property
	 *
	 * @return The property name
	 */
	String getName();

	/**
	 * The type of the property
	 *
	 * @return The property type
	 */
	Class<?> getType();

	/**
	 * Returns the {@link TypeInformation} of the property.
	 *
	 * @return
	 */
	TypeInformation<?> getTypeInformation();

	/**
	 * Returns the {@link TypeInformation} if the property references a {@link PersistentEntity}. Will return
	 * {@literal null} in case it refers to a simple type. Will return {@link Collection}'s component type or the
	 * {@link Map}'s value type transparently.
	 *
	 * @return
	 */
	Iterable<? extends TypeInformation<?>> getPersistentEntityTypes();

	/**
	 * Returns the getter method to access the property value if available. Might return {@literal null} in case there is
	 * no getter method with a return type assignable to the actual property's type.
	 *
	 * @return the getter method to access the property value if available, otherwise {@literal null}.
	 */
	@Nullable
	Method getGetter();

	default Method getRequiredGetter() {

		Method getter = getGetter();

		if (getter == null) {
			throw new IllegalArgumentException(String.format("No getter available for persistent property %s!", this));
		}

		return getter;
	}

	/**
	 * Returns the setter method to set a property value. Might return {@literal null} in case there is no setter
	 * available.
	 *
	 * @return the setter method to set a property value if available, otherwise {@literal null}.
	 */
	@Nullable
	Method getSetter();

	default Method getRequiredSetter() {

		Method setter = getSetter();

		if (setter == null) {
			throw new IllegalArgumentException(String.format("No setter available for persistent property %s!", this));
		}

		return setter;
	}

	/**
	 * Returns the with {@link Method} to set a property value on a new object instance. Might return {@literal null} in
	 * case there is no with available.
	 * <p/>
	 * With {@link Method methods} are property-bound instance {@link Method methods} that accept a single argument of the
	 * property type creating a new object instance.
	 *
	 * <pre class="code">
	 * class Person {
	 * 	final String id;
	 * 	final String name;
	 *
	 * 	// …
	 *
	 * 	Person withName(String name) {
	 * 		return new Person(this.id, name);
	 * 	}
	 * }
	 * </pre>
	 *
	 * @return the with {@link Method} to set a property value on a new object instance if available, otherwise
	 *         {@literal null}.
	 * @since 2.1
	 */
	@Nullable
	Method getWither();

	default Method getRequiredWither() {

		Method wither = getWither();

		if (wither == null) {
			throw new IllegalArgumentException(String.format("No wither available for persistent property %s!", this));
		}

		return wither;
	}

	@Nullable
	Field getField();

	default Field getRequiredField() {

		Field field = getField();

		if (field == null) {
			throw new IllegalArgumentException(String.format("No field backing persistent property %s!", this));
		}

		return field;
	}

	/**
	 * @return {@literal null} if no expression defined.
	 */
	@Nullable
	String getSpelExpression();

	/**
	 * @return {@literal null} if property is not part of an {@link Association}.
	 */
	@Nullable
	Association<P> getAssociation();

	/**
	 * Get the {@link Association} of this property.
	 *
	 * @return never {@literal null}.
	 * @throws IllegalStateException if not involved in an {@link Association}.
	 */
	default Association<P> getRequiredAssociation() {

		Association<P> association = getAssociation();

		if (association != null) {
			return association;
		}

		throw new IllegalStateException("No association found!");
	}

	/**
	 * Returns whether the type of the {@link PersistentProperty} is actually to be regarded as {@link PersistentEntity}
	 * in turn.
	 *
	 * @return {@literal true} a {@link PersistentEntity}.
	 */
	boolean isEntity();

	/**
	 * Returns whether the property is a <em>potential</em> identifier property of the owning {@link PersistentEntity}.
	 * This method is mainly used by {@link PersistentEntity} implementation to discover id property candidates on
	 * {@link PersistentEntity} creation you should rather call {@link PersistentEntity#isIdProperty(PersistentProperty)}
	 * to determine whether the current property is the id property of that {@link PersistentEntity} under consideration.
	 *
	 * @return {@literal true} if the {@literal id} property.
	 */
	boolean isIdProperty();

	/**
	 * Returns whether the current property is a <em>potential</em> version property of the owning
	 * {@link PersistentEntity}. This method is mainly used by {@link PersistentEntity} implementation to discover version
	 * property candidates on {@link PersistentEntity} creation you should rather call
	 * {@link PersistentEntity#isVersionProperty(PersistentProperty)} to determine whether the current property is the
	 * version property of that {@link PersistentEntity} under consideration.
	 *
	 * @return
	 */
	boolean isVersionProperty();

	/**
	 * Returns whether the property is a {@link Collection}, {@link Iterable} or an array.
	 *
	 * @return
	 */
	boolean isCollectionLike();

	/**
	 * Returns whether the property is a {@link Map}.
	 *
	 * @return
	 */
	boolean isMap();

	/**
	 * Returns whether the property is an array.
	 *
	 * @return
	 */
	boolean isArray();

	/**
	 * Returns whether the property is transient.
	 *
	 * @return
	 */
	boolean isTransient();

	/**
	 * Returns whether the current property is writable, i.e. if the value held for it shall be written to the data store.
	 *
	 * @return
	 * @since 1.9
	 */
	boolean isWritable();

	/**
	 * Returns whether the current property is immutable, i.e. if there is no setter or the backing {@link Field} is
	 * {@code final}.
	 *
	 * @return
	 * @see java.lang.reflect.Modifier#isFinal(int)
	 * @since 2.1
	 */
	boolean isImmutable();

	/**
	 * Returns whether the property is an {@link Association}.
	 *
	 * @return
	 */
	boolean isAssociation();

	/**
	 * Returns the component type of the type if it is a {@link java.util.Collection}. Will return the type of the key if
	 * the property is a {@link java.util.Map}.
	 *
	 * @return the component type, the map's key type or {@literal null} if neither {@link java.util.Collection} nor
	 *         {@link java.util.Map}.
	 */
	@Nullable
	Class<?> getComponentType();

	/**
	 * Returns the raw type as it's pulled from from the reflected property.
	 *
	 * @return the raw type of the property.
	 */
	Class<?> getRawType();

	/**
	 * Returns the type of the values if the property is a {@link java.util.Map}.
	 *
	 * @return the map's value type or {@literal null} if no {@link java.util.Map}
	 */
	@Nullable
	Class<?> getMapValueType();

	/**
	 * Returns the actual type of the property. This will be the original property type if no generics were used, the
	 * component type for collection-like types and arrays as well as the value type for map properties.
	 *
	 * @return
	 */
	Class<?> getActualType();

	/**
	 * Looks up the annotation of the given type on the {@link PersistentProperty}. Will inspect accessors and the
	 * potentially backing field and traverse accessor methods to potentially available super types.
	 *
	 * @param annotationType the annotation to look up, must not be {@literal null}.
	 * @return the annotation of the given type. Can be {@literal null}.
	 * @see AnnotationUtils#findAnnotation(Method, Class)
	 */
	@Nullable
	<A extends Annotation> A findAnnotation(Class<A> annotationType);

	/**
	 * Looks up the annotation of the given type on the {@link PersistentProperty}. Will inspect accessors and the
	 * potentially backing field and traverse accessor methods to potentially available super types.
	 *
	 * @param annotationType the annotation to look up, must not be {@literal null}.
	 * @return the annotation of the given type.
	 * @throws IllegalStateException if the required {@code annotationType} is not found.
	 * @since 2.0
	 */
	default <A extends Annotation> A getRequiredAnnotation(Class<A> annotationType) throws IllegalStateException {

		A annotation = findAnnotation(annotationType);

		if (annotation != null) {
			return annotation;
		}

		throw new IllegalStateException(
				String.format("Required annotation %s not found for %s!", annotationType, getName()));
	}

	/**
	 * Looks up the annotation of the given type on the property and the owning type if no annotation can be found on it.
	 * Useful to lookup annotations that can be configured on the type but overridden on an individual property.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return the annotation of the given type. Can be {@literal null}.
	 */
	@Nullable
	<A extends Annotation> A findPropertyOrOwnerAnnotation(Class<A> annotationType);

	/**
	 * Returns whether the {@link PersistentProperty} has an annotation of the given type.
	 *
	 * @param annotationType the annotation to lookup, must not be {@literal null}.
	 * @return whether the {@link PersistentProperty} has an annotation of the given type.
	 */
	boolean isAnnotationPresent(Class<? extends Annotation> annotationType);

	/**
	 * Returns whether property access shall be used for reading the property value. This means it will use the getter
	 * instead of field access.
	 *
	 * @return
	 */
	boolean usePropertyAccess();

	/**
	 * Returns whether the actual type of the property carries the given annotation.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return
	 * @since 2.1
	 * @see #getActualType()
	 */
	default boolean hasActualTypeAnnotation(Class<? extends Annotation> annotationType) {

		Assert.notNull(annotationType, "Annotation type must not be null!");

		return AnnotatedElementUtils.hasAnnotation(getActualType(), annotationType);
	}

	/**
	 * Return the type the property refers to in case it's an association.
	 *
	 * @return the type the property refers to in case it's an association or {@literal null} in case it's not an
	 *         association, the target entity type is not explicitly defined (either explicitly or through the property
	 *         type itself).
	 * @since 2.1
	 */
	@Nullable
	Class<?> getAssociationTargetType();

	/**
	 * Returns a {@link PersistentPropertyAccessor} for the current property's owning value.
	 *
	 * @param owner must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @since 2.3
	 */
	default <T> PersistentPropertyAccessor<T> getAccessorForOwner(T owner) {

		Assert.notNull(owner, "Owner must not be null!");

		return getOwner().getPropertyAccessor(owner);
	}
}
