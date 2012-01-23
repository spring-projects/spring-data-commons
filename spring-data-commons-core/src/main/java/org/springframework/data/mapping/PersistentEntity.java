/*
 * Copyright (c) 2011-2012 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import org.springframework.data.util.TypeInformation;

/**
 * Represents a persistent entity
 * 
 * @author Oliver Gierke
 * @author Graeme Rocher
 * @author Jon Brisbin
 */
public interface PersistentEntity<T, P extends PersistentProperty<P>> {

	/**
	 * The entity name including any package prefix
	 * 
	 * @return must never return {@literal null}
	 */
	String getName();

	/**
	 * Returns the {@link PreferredConstructor} to be used to instantiate objects of this {@link PersistentEntity}.
	 * 
	 * @return {@literal null} in case no suitable constructor for automatic construction can be found.
	 */
	PreferredConstructor<T, P> getPersistenceConstructor();

	/**
	 * Returns whether the given {@link PersistentProperty} is referred to by a constructor argument of the
	 * {@link PersistentEntity}.
	 * 
	 * @param property
	 * @return true if the given {@link PersistentProperty} is referred to by a constructor argument or {@literal false} if
	 *         not or {@literal null}.
	 */
	boolean isConstructorArgument(P property);

	/**
	 * Returns the id property of the {@link PersistentEntity}. Must never return {@literal null} as a
	 * {@link PersistentEntity} instance must not be created if there is no id property.
	 * 
	 * @return the id property of the {@link PersistentEntity}.
	 */
	P getIdProperty();

	/**
	 * Obtains a PersistentProperty instance by name.
	 * 
	 * @param name The name of the property
	 * @return The {@link PersistentProperty} or {@literal null} if it doesn't exist
	 */
	P getPersistentProperty(String name);

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
	Object getTypeAlias();

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

	/**
	 * Applies the given {@link AssociationHandler} to all {@link Association} contained in this {@link PersistentEntity}.
	 * 
	 * @param handler must not be {@literal null}.
	 */
	void doWithAssociations(AssociationHandler<P> handler);
}
