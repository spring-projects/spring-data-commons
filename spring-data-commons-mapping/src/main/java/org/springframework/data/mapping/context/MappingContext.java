/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.mapping.context;

import java.util.Collection;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
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
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Johannes Englmeier
 */
public interface MappingContext<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	/**
	 * Returns all {@link PersistentEntity}s held in the context.
	 *
	 * @return never {@literal null}.
	 */
	Collection<E> getPersistentEntities();

	/**
	 * Returns a {@link PersistentEntity} for the given {@link Class}. Will return {@code null} for types that are
	 * considered simple ones.
	 *
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return {@literal null} if no {@link PersistentEntity} found for {@literal type}.
	 */
	@Nullable
	E getPersistentEntity(Class<?> type);

	/**
	 * Returns a required {@link PersistentEntity} for the given {@link Class}. Will throw
	 * {@link IllegalArgumentException} for types that are considered simple ones.
	 *
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return never {@literal null}.
	 * @throws MappingException when no {@link PersistentEntity} can be found for given {@literal type}.
	 * @since 2.0
	 */
	default E getRequiredPersistentEntity(Class<?> type) throws MappingException {

		E entity = getPersistentEntity(type);

		if (entity != null) {
			return entity;
		}

		throw new MappingException(String.format("Couldn't find PersistentEntity for type %s", type));
	}

	/**
	 * Returns whether the {@link MappingContext} currently contains a {@link PersistentEntity} for the type.
	 *
	 * @param type must not be {@literal null}.
	 * @return {@literal true} if {@link PersistentEntity} present for given {@literal type}.
	 * @since 1.8
	 */
	boolean hasPersistentEntityFor(Class<?> type);

	/**
	 * Returns a {@link PersistentEntity} for the given {@link TypeInformation}. Will return {@code null} for types that
	 * are considered simple ones.
	 *
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return {@literal null} if no {@link PersistentEntity} found for {@link TypeInformation}.
	 */
	@Nullable
	E getPersistentEntity(TypeInformation<?> type);

	/**
	 * Returns a {@link PersistentEntity} for the given {@link TypeInformation}. Will throw
	 * {@link IllegalArgumentException} for types that are considered simple ones.
	 *
	 * @see org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)
	 * @param type must not be {@literal null}.
	 * @return never {@literal null}.
	 * @throws MappingException when no {@link PersistentEntity} can be found for given {@link TypeInformation}.
	 */
	default E getRequiredPersistentEntity(TypeInformation<?> type) throws MappingException {

		E entity = getPersistentEntity(type);

		if (entity != null) {
			return entity;
		}

		throw new MappingException(String.format("Couldn't find PersistentEntity for type %s", type));
	}

	/**
	 * Returns the {@link PersistentEntity} mapped by the given {@link PersistentProperty}.
	 *
	 * @param persistentProperty must not be {@literal null}.
	 * @return the {@link PersistentEntity} mapped by the given {@link PersistentProperty} or {@literal null} if no
	 *         {@link PersistentEntity} exists for it or the {@link PersistentProperty} does not refer to an entity (the
	 *         type of the property is considered simple see
	 *         {@link org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)}).
	 */
	@Nullable
	E getPersistentEntity(P persistentProperty);

	/**
	 * Returns the {@link PersistentEntity} mapped by the given {@link PersistentProperty}.
	 *
	 * @param persistentProperty must not be {@literal null}.
	 * @return the {@link PersistentEntity} mapped by the given {@link PersistentProperty} or {@literal null} if no
	 *         {@link PersistentEntity} exists for it or the {@link PersistentProperty} does not refer to an entity (the
	 *         type of the property is considered simple see
	 *         {@link org.springframework.data.mapping.model.SimpleTypeHolder#isSimpleType(Class)}).
	 * @throws MappingException when no {@link PersistentEntity} can be found for given {@link PersistentProperty}.
	 */
	default E getRequiredPersistentEntity(P persistentProperty) throws MappingException {

		E entity = getPersistentEntity(persistentProperty);

		if (entity != null) {
			return entity;
		}

		throw new MappingException(String.format("Couldn't find PersistentEntity for property %s", persistentProperty));
	}

	/**
	 * Returns all {@link PersistentProperty}s for the given path expression based on the given {@link PropertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} representing the given {@link PropertyPath}.
	 * @throws InvalidPersistentPropertyPath in case not all segments of the given {@link PropertyPath} can be resolved.
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(PropertyPath propertyPath) throws InvalidPersistentPropertyPath;

	/**
	 * Returns all {@link PersistentProperty}s for the given dot path notation based on the given type.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} representing the given property path on the given type.
	 * @throws InvalidPersistentPropertyPath in case not all segments of the given property path can be resolved.
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(String propertyPath, Class<?> type)
			throws InvalidPersistentPropertyPath;

	/**
	 * Returns all {@link PersistentProperty}s for the given dot path notation based on the given type.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return the {@link PersistentPropertyPath} representing the given property path on the given type.
	 * @throws InvalidPersistentPropertyPath in case not all segments of the given property path can be resolved.
	 * @since 3.2.1
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(String propertyPath, TypeInformation<?> type)
			throws InvalidPersistentPropertyPath;

	/**
	 * Returns all {@link PersistentPropertyPath}s pointing to properties on the given type that match the given
	 * {@link Predicate}. In case of circular references the detection will stop at the property that references a type
	 * that's already included in the path. Note, that is a potentially expensive operation as results cannot be cached.
	 *
	 * @param type must not be {@literal null}.
	 * @param predicate must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	<T> PersistentPropertyPaths<T, P> findPersistentPropertyPaths(Class<T> type, Predicate<? super P> predicate);

	/**
	 * Returns the {@link TypeInformation}s for all {@link PersistentEntity}s in the {@link MappingContext}.
	 *
	 * @return all {@link TypeInformation}s for the {@link PersistentEntity}s in the {@link MappingContext}.
	 * @since 1.8
	 */
	Collection<TypeInformation<?>> getManagedTypes();
}
