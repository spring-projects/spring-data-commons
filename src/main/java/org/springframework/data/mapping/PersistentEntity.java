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
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.util.TypeInformation;

/**
 * Represents a persistent entity.
 * 
 * @author Oliver Gierke
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Patryk Wasik
 */
public interface PersistentEntity<T, P extends PersistentProperty<P>> {

	/**
	 * The entity name including any package prefix.
	 * 
	 * @return must never return {@literal null}
	 */
	String getName();

	/**
	 * Returns the {@link PreferredConstructor} to be used to instantiate objects of this {@link PersistentEntity}.
	 * 
	 * @return An empty {@link Optional} in case no suitable constructor for automatic construction can be found. This
	 *         usually indicates that the instantiation of the object of that persistent entity is done through either a
	 *         customer {@link EntityInstantiator} or handled by custom conversion mechanisms entirely.
	 */
	Optional<PreferredConstructor<T, P>> getPersistenceConstructor();

	/**
	 * Returns whether the given {@link PersistentProperty} is referred to by a constructor argument of the
	 * {@link PersistentEntity}.
	 * 
	 * @param property
	 * @return true if the given {@link PersistentProperty} is referred to by a constructor argument or {@literal false}
	 *         if not or {@literal null}.
	 */
	boolean isConstructorArgument(PersistentProperty<?> property);

	/**
	 * Returns whether the given {@link PersistentProperty} is the id property of the entity.
	 * 
	 * @param property
	 * @return
	 */
	boolean isIdProperty(PersistentProperty<?> property);

	/**
	 * Returns whether the given {@link PersistentProperty} is the version property of the entity.
	 * 
	 * @param property
	 * @return
	 */
	boolean isVersionProperty(PersistentProperty<?> property);

	/**
	 * Returns the id property of the {@link PersistentEntity}. Can be {@literal null} in case this is an entity
	 * completely handled by a custom conversion.
	 * 
	 * @return the id property of the {@link PersistentEntity}.
	 */
	Optional<P> getIdProperty();

	/**
	 * Returns the version property of the {@link PersistentEntity}. Can be {@literal null} in case no version property is
	 * available on the entity.
	 * 
	 * @return the version property of the {@link PersistentEntity}.
	 */
	Optional<P> getVersionProperty();

	/**
	 * Obtains a {@link PersistentProperty} instance by name.
	 * 
	 * @param name The name of the property
	 * @return the {@link PersistentProperty} or {@literal null} if it doesn't exist.
	 */
	Optional<P> getPersistentProperty(String name);

	P getRequiredPersistentProperty(String name);

	/**
	 * Returns the property equipped with an annotation of the given type.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	Optional<P> getPersistentProperty(Class<? extends Annotation> annotationType);

	/**
	 * Returns whether the {@link PersistentEntity} has an id property. If this call returns {@literal true},
	 * {@link #getIdProperty()} will return a non-{@literal null} value.
	 * 
	 * @return
	 */
	boolean hasIdProperty();

	/**
	 * Returns whether the {@link PersistentEntity} has a version property. If this call returns {@literal true},
	 * {@link #getVersionProperty()} will return a non-{@literal null} value.
	 * 
	 * @return
	 */
	boolean hasVersionProperty();

	/**
	 * Returns the resolved Java type of this entity.
	 * 
	 * @return The underlying Java class for this entity
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

	Stream<P> getPersistentProperties();

	/**
	 * Applies the given {@link AssociationHandler} to all {@link Association} contained in this {@link PersistentEntity}.
	 * 
	 * @param handler must not be {@literal null}.
	 */
	void doWithAssociations(AssociationHandler<P> handler);

	void doWithAssociations(SimpleAssociationHandler handler);

	Stream<Association<P>> getAssociations();

	/**
	 * Looks up the annotation of the given type on the {@link PersistentEntity}.
	 * 
	 * @param annotationType must not be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	<A extends Annotation> Optional<A> findAnnotation(Class<A> annotationType);

	/**
	 * Returns a {@link PersistentPropertyAccessor} to access property values of the given bean.
	 * 
	 * @param bean must not be {@literal null}.
	 * @return
	 * @since 1.10
	 */
	PersistentPropertyAccessor getPropertyAccessor(Object bean);

	/**
	 * Returns the {@link IdentifierAccessor} for the given bean.
	 * 
	 * @param bean must not be {@literal null}.
	 * @return
	 * @since 1.10
	 */
	IdentifierAccessor getIdentifierAccessor(Object bean);
}
