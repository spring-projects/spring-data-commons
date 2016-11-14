/*
 * Copyright 2011-2014 the original author or authors.
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
package org.springframework.data.mapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.util.TypeInformation;

/**
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
 */
public interface PersistentProperty<P extends PersistentProperty<P>> {

	/**
	 * Returns the {@link PersistentEntity} owning the current {@link PersistentProperty}.
	 * 
	 * @return
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
	Iterable<? extends TypeInformation<?>> getPersistentEntityType();

	/**
	 * Returns the getter method to access the property value if available. Might return {@literal null} in case there is
	 * no getter method with a return type assignable to the actual property's type.
	 * 
	 * @return the getter method to access the property value if available, otherwise {@literal null}.
	 */
	Optional<Method> getGetter();

	/**
	 * Returns the setter method to set a property value. Might return {@literal null} in case there is no setter
	 * available.
	 * 
	 * @return the setter method to set a property value if available, otherwise {@literal null}.
	 */
	Optional<Method> getSetter();

	Optional<Field> getField();

	Optional<String> getSpelExpression();

	Association<P> getAssociation();

	/**
	 * Returns whether the type of the {@link PersistentProperty} is actually to be regarded as {@link PersistentEntity}
	 * in turn.
	 * 
	 * @return
	 */
	boolean isEntity();

	/**
	 * Returns whether the property is a <em>potential</em> identifier property of the owning {@link PersistentEntity}.
	 * This method is mainly used by {@link PersistentEntity} implementation to discover id property candidates on
	 * {@link PersistentEntity} creation you should rather call {@link PersistentEntity#isIdProperty(PersistentProperty)}
	 * to determine whether the current property is the id property of that {@link PersistentEntity} under consideration.
	 * 
	 * @return
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
	 * @return the annotation of the given type if present or {@literal null} otherwise.
	 * @see AnnotationUtils#findAnnotation(Method, Class)
	 */
	<A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType);

	/**
	 * Looks up the annotation of the given type on the property and the owning type if no annotation can be found on it.
	 * Usefull to lookup annotations that can be configured on the type but overridden on an individual property.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @return
	 */
	<A extends Annotation> Optional<A> findPropertyOrOwnerAnnotation(Class<A> annotationType);

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
}
