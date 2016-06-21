/*
 * Copyright 2011-2015 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;

/**
 * This interface defines the overall context including all known PersistentEntity instances and methods to obtain
 * instances on demand. it is used internally to establish associations between entities and also at runtime to obtain
 * entities by name.
 * 
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Graeme Rocher
 */
public interface MappingContext<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	/**
	 * Returns all {@link PersistentEntity}s held in the context.
	 * 
	 * @return
	 */
	Collection<E> getPersistentEntities();

	/**
	 * Returns a {@link PersistentEntity} for the given {@link Class}. Will return {@literal null} for types that are
	 * considered simple ones.
	 * 
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return
	 */
	Optional<E> getPersistentEntity(Class<?> type);

	E getRequiredPersistentEntity(Class<?> type);

	/**
	 * Returns whether the {@link MappingContext} currently contains a {@link PersistentEntity} for the type.
	 * 
	 * @param type can be {@literal null}.
	 * @return
	 * @since 1.8
	 */
	boolean hasPersistentEntityFor(Class<?> type);

	/**
	 * Returns a {@link PersistentEntity} for the given {@link TypeInformation}. Will return {@literal null} for types
	 * that are considered simple ones.
	 * 
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return
	 */
	Optional<E> getPersistentEntity(TypeInformation<?> type);

	E getRequiredPersistentEntity(TypeInformation<?> type);

	/**
	 * Returns the {@link PersistentEntity} mapped by the given {@link PersistentProperty}.
	 * 
	 * @param persistentProperty must not be {@literal null}.
	 * @return the {@link PersistentEntity} mapped by the given {@link PersistentProperty} or null if no
	 *         {@link PersistentEntity} exists for it or the {@link PersistentProperty} does not refer to an entity (the
	 *         type of the property is considered simple see
	 *         {@link org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)}).
	 */
	Optional<E> getPersistentEntity(P persistentProperty);

	E getRequiredPersistentEntity(P persistentProperty);

	/**
	 * Returns all {@link PersistentProperty}s for the given path expression based on the given {@link PropertyPath}.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} representing the given {@link PropertyPath}.
	 * @throws InvalidPersistentPropertyPath in case not all of the segments of the given {@link PropertyPath} can be
	 *           resolved.
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(PropertyPath propertyPath);

	/**
	 * Returns all {@link PersistentProperty}s for the given dot path notation based on the given type.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} representing the given property path on the given type.
	 * @throws InvalidPersistentPropertyPath in case not all of the segments of the given property path can be resolved.
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(String propertyPath, Class<?> type);

	/**
	 * Returns the {@link PersistentPropertyPath} for the resolvable part of the given
	 * {@link InvalidPersistentPropertyPath}.
	 * 
	 * @param invalidPath must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} for the resolvable part of the given
	 *         {@link InvalidPersistentPropertyPath}.
	 * @since 1.11
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(InvalidPersistentPropertyPath invalidPath);

	/**
	 * Returns the {@link TypeInformation}s for all {@link PersistentEntity}s in the {@link MappingContext}.
	 * 
	 * @return all {@link TypeInformation}s for the {@link PersistentEntity}s in the {@link MappingContext}.
	 * @since 1.8
	 */
	Collection<TypeInformation<?>> getManagedTypes();
}
