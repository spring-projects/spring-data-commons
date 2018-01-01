/*
 * Copyright 2011-2018 the original author or authors.
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
import java.util.Iterator;

import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * Represents a persistent entity.
 *
 * @author Oliver Gierke
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Patryk Wasik
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public interface PersistentEntity<T, P extends PersistentProperty<P>> extends Iterable<P> {

	/**
	 * The entity name including any package prefix.
	 *
	 * @return must never return {@literal null}.
	 */
	String getName();

	/**
	 * Returns the {@link PreferredConstructor} to be used to instantiate objects of this {@link PersistentEntity}.
	 *
	 * @return {@literal null} in case no suitable constructor for automatic construction can be found. This usually
	 *         indicates that the instantiation of the object of that persistent entity is done through either a customer
	 *         {@link EntityInstantiator} or handled by custom conversion mechanisms entirely.
	 */
	@Nullable
	PreferredConstructor<T, P> getPersistenceConstructor();

	/**
	 * Returns whether the given {@link PersistentProperty} is referred to by a constructor argument of the
	 * {@link PersistentEntity}.
	 *
	 * @param property can be {@literal null}.
	 * @return true if the given {@link PersistentProperty} is referred to by a constructor argument or {@literal false}
	 *         if not or {@literal null}.
	 */
	boolean isConstructorArgument(PersistentProperty<?> property);

	/**
	 * Returns whether the given {@link PersistentProperty} is the id property of the entity.
	 *
	 * @param property can be {@literal null}.
	 * @return {@literal true} given property is the entities id.
	 */
	boolean isIdProperty(PersistentProperty<?> property);

	/**
	 * Returns whether the given {@link PersistentProperty} is the version property of the entity.
	 *
	 * @param property can be {@literal null}.
	 * @return {@literal true} given property is used as version.
	 */
	boolean isVersionProperty(PersistentProperty<?> property);

	/**
	 * Returns the id property of the {@link PersistentEntity}. Can be {@literal null} in case this is an entity
	 * completely handled by a custom conversion.
	 *
	 * @return the id property of the {@link PersistentEntity}.
	 */
	@Nullable
	P getIdProperty();

	/**
	 * Returns the id property of the {@link PersistentEntity}.
	 *
	 * @return the id property of the {@link PersistentEntity}.
	 * @throws IllegalStateException if {@link PersistentEntity} does not define an {@literal id} property.
	 * @since 2.0
	 */
	default P getRequiredIdProperty() {

		P property = getIdProperty();

		if (property != null) {
			return property;
		}

		throw new IllegalStateException(String.format("Required identifier property not found for %s!", getType()));
	}

	/**
	 * Returns the version property of the {@link PersistentEntity}. Can be {@literal null} in case no version property is
	 * available on the entity.
	 *
	 * @return the version property of the {@link PersistentEntity}.
	 */
	@Nullable
	P getVersionProperty();

	/**
	 * Returns the version property of the {@link PersistentEntity}. Can be {@literal null} in case no version property is
	 * available on the entity.
	 *
	 * @return the version property of the {@link PersistentEntity}.
	 * @throws IllegalStateException if {@link PersistentEntity} does not define a {@literal version} property.
	 * @since 2.0
	 */
	default P getRequiredVersionProperty() {

		P property = getVersionProperty();

		if (property != null) {
			return property;
		}

		throw new IllegalStateException(String.format("Required version property not found for %s!", getType()));
	}

	/**
	 * Obtains a {@link PersistentProperty} instance by name.
	 *
	 * @param name The name of the property. Can be {@literal null}.
	 * @return the {@link PersistentProperty} or {@literal null} if it doesn't exist.
	 */
	@Nullable
	P getPersistentProperty(String name);

	/**
	 * Returns the {@link PersistentProperty} with the given name.
	 *
	 * @param name the name of the property. Can be {@literal null} or empty.
	 * @return the {@link PersistentProperty} with the given name.
	 * @throws IllegalStateException in case no property with the given name exists.
	 */
	default P getRequiredPersistentProperty(String name) {

		P property = getPersistentProperty(name);

		if (property != null) {
			return property;
		}

		throw new IllegalStateException(String.format("Required property %s not found for %s!", name, getType()));
	}

	/**
	 * Returns the first property equipped with an {@link Annotation} of the given type.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return {@literal null} if no property found with given annotation type.
	 * @since 1.8
	 */
	@Nullable
	default P getPersistentProperty(Class<? extends Annotation> annotationType) {

		Iterator<P> it = getPersistentProperties(annotationType).iterator();
		return it.hasNext() ? it.next() : null;
	}

	/**
	 * Returns all properties equipped with an {@link Annotation} of the given type.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return {@literal empty} {@link Iterator} if no match found. Never {@literal null}.
	 * @since 2.0
	 */
	Iterable<P> getPersistentProperties(Class<? extends Annotation> annotationType);

	/**
	 * Returns whether the {@link PersistentEntity} has an id property. If this call returns {@literal true},
	 * {@link #getIdProperty()} will return a non-{@literal null} value.
	 *
	 * @return {@literal true} if entity has an {@literal id} property.
	 */
	boolean hasIdProperty();

	/**
	 * Returns whether the {@link PersistentEntity} has a version property. If this call returns {@literal true},
	 * {@link #getVersionProperty()} will return a non-{@literal null} value.
	 *
	 * @return {@literal true} if entity has a {@literal version} property.
	 */
	boolean hasVersionProperty();

	/**
	 * Returns the resolved Java type of this entity.
	 *
	 * @return The underlying Java class for this entity. Never {@literal null}.
	 */
	Class<T> getType();

	/**
	 * Returns the alias to be used when storing type information. Might be {@literal null} to indicate that there was no
	 * alias defined through the mapping metadata.
	 *
	 * @return
	 */
	Alias getTypeAlias();

	/**
	 * Returns the {@link TypeInformation} backing this {@link PersistentEntity}.
	 *
	 * @return
	 */
	TypeInformation<T> getTypeInformation();

	/**
	 * Applies the given {@link PropertyHandler} to all {@link PersistentProperty}s contained in this
	 * {@link PersistentEntity}.
	 *
	 * @param handler must not be {@literal null}.
	 */
	void doWithProperties(PropertyHandler<P> handler);

	void doWithProperties(SimplePropertyHandler handler);

	/**
	 * Applies the given {@link AssociationHandler} to all {@link Association} contained in this {@link PersistentEntity}.
	 *
	 * @param handler must not be {@literal null}.
	 */
	void doWithAssociations(AssociationHandler<P> handler);

	void doWithAssociations(SimpleAssociationHandler handler);

	/**
	 * Looks up the annotation of the given type on the {@link PersistentEntity}.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return {@literal null} if not found.
	 * @since 1.8
	 */
	@Nullable
	<A extends Annotation> A findAnnotation(Class<A> annotationType);

	/**
	 * Returns the required annotation of the given type on the {@link PersistentEntity}.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return the annotation.
	 * @throws IllegalStateException if the required {@code annotationType} is not found.
	 * @since 2.0
	 */
	default <A extends Annotation> A getRequiredAnnotation(Class<A> annotationType) throws IllegalStateException {

		A annotation = findAnnotation(annotationType);

		if (annotation != null) {
			return annotation;
		}

		throw new IllegalStateException(
				String.format("Required annotation %s not found for %s!", annotationType, getType()));
	}

	/**
	 * Checks whether the annotation of the given type is present on the {@link PersistentEntity}.
	 *
	 * @param annotationType must not be {@literal null}.
	 * @return {@literal true} if {@link Annotation} of given type is present.
	 * @since 2.0
	 */
	<A extends Annotation> boolean isAnnotationPresent(Class<A> annotationType);

	/**
	 * Returns a {@link PersistentPropertyAccessor} to access property values of the given bean.
	 *
	 * @param bean must not be {@literal null}.
	 * @return new {@link PersistentPropertyAccessor}.
	 * @since 1.10
	 */
	PersistentPropertyAccessor getPropertyAccessor(Object bean);

	/**
	 * Returns the {@link IdentifierAccessor} for the given bean.
	 *
	 * @param bean must not be {@literal null}.
	 * @return new {@link IdentifierAccessor}.
	 * @since 1.10
	 */
	IdentifierAccessor getIdentifierAccessor(Object bean);
}
