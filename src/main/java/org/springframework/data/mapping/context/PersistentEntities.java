/*
 * Copyright 2014-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object to access {@link PersistentEntity} instances managed by {@link MappingContext}s.
 *
 * @author Oliver Gierke
 * @since 1.8
 */
public class PersistentEntities implements Streamable<PersistentEntity<?, ? extends PersistentProperty<?>>> {

	private final Streamable<? extends MappingContext<?, ? extends PersistentProperty<?>>> contexts;

	/**
	 * Creates a new {@link PersistentEntities} for the given {@link MappingContext}s.
	 *
	 * @param contexts
	 */
	public PersistentEntities(Iterable<? extends MappingContext<?, ?>> contexts) {

		Assert.notNull(contexts, "MappingContexts must not be null!");

		this.contexts = Streamable.of(contexts);
	}

	/**
	 * Creates a new {@link PersistentEntities} for the given {@link MappingContext}s.
	 * 
	 * @param contexts must not be {@literal null}.
	 * @return
	 */
	public static PersistentEntities of(MappingContext<?, ?>... contexts) {

		Assert.notNull(contexts, "MappingContexts must not be null!");

		return new PersistentEntities(Arrays.asList(contexts));
	}

	/**
	 * Returns the {@link PersistentEntity} for the given type. Will consider all {@link MappingContext}s registered but
	 * return {@literal Optional#empty()} in case none of the registered ones already have a {@link PersistentEntity}
	 * registered for the given type.
	 *
	 * @param type can be {@literal null}.
	 * @return
	 */
	public Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> getPersistentEntity(Class<?> type) {

		return contexts.stream()//
				.filter(it -> it.hasPersistentEntityFor(type))//
				.findFirst().map(it -> it.getRequiredPersistentEntity(type));
	}

	/**
	 * Returns the {@link PersistentEntity} for the given type. Will consider all {@link MappingContext}s registered but
	 * throw an {@link IllegalArgumentException} in case none of the registered ones already have a
	 * {@link PersistentEntity} registered for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return the {@link PersistentEntity} for the given domain type.
	 * @throws IllegalArgumentException in case no {@link PersistentEntity} can be found for the given type.
	 */
	public PersistentEntity<?, ? extends PersistentProperty<?>> getRequiredPersistentEntity(Class<?> type) {

		Assert.notNull(type, "Domain type must not be null!");

		return getPersistentEntity(type).orElseThrow(
				() -> new IllegalArgumentException(String.format("Couldn't find PersistentEntity for type %s!", type)));
	}

	/**
	 * Executes the given {@link BiFunction} on the given {@link MappingContext} and {@link PersistentEntity} based on the
	 * given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @param combiner must not be {@literal null}.
	 * @return
	 */
	public <T> Optional<T> mapOnContext(Class<?> type,
			BiFunction<MappingContext<?, ? extends PersistentProperty<?>>, PersistentEntity<?, ?>, T> combiner) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(combiner, "Combining BiFunction must not be null!");

		return contexts.stream() //
				.filter(it -> it.hasPersistentEntityFor(type)) //
				.map(it -> combiner.apply(it, it.getRequiredPersistentEntity(type))) //
				.findFirst();
	}

	/**
	 * Returns all {@link TypeInformation} exposed by the registered {@link MappingContext}s.
	 *
	 * @return
	 */
	public Streamable<TypeInformation<?>> getManagedTypes() {

		return Streamable.of(contexts.stream()//
				.flatMap(it -> it.getManagedTypes().stream())//
				.collect(Collectors.toSet()));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<PersistentEntity<?, ? extends PersistentProperty<?>>> iterator() {

		return contexts.stream()
				.<PersistentEntity<?, ? extends PersistentProperty<?>>> flatMap(it -> it.getPersistentEntities().stream())
				.collect(Collectors.toList()).iterator();
	}

	/**
	 * Returns the {@link PersistentEntity} the given {@link PersistentProperty} refers to in case it's an association.
	 * For direct aggregate references, that's simply the entity for the {@link PersistentProperty}'s actual type. If the
	 * property type is not an entity - as it might rather refer to the identifier type - we either use the reference's
	 * defined target type and fall back to trying to find a {@link PersistentEntity} identified by the
	 * {@link PersistentProperty}'s actual type.
	 * 
	 * @param property must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	@Nullable
	public PersistentEntity<?, ?> getEntityUltimatelyReferredToBy(PersistentProperty<?> property) {

		TypeInformation<?> propertyType = property.getTypeInformation().getActualType();

		if (propertyType == null || !property.isAssociation()) {
			return null;
		}

		Class<?> associationTargetType = property.getAssociationTargetType();

		return associationTargetType == null //
				? getEntityIdentifiedBy(propertyType) //
				: getPersistentEntity(associationTargetType).orElseGet(() -> getEntityIdentifiedBy(propertyType));
	}

	/**
	 * Returns the type the given {@link PersistentProperty} ultimately refers to. In case it's of a unique identifier
	 * type of an entity known it'll return the entity type.
	 * 
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public TypeInformation<?> getTypeUltimatelyReferredToBy(PersistentProperty<?> property) {

		Assert.notNull(property, "PersistentProperty must not be null!");

		PersistentEntity<?, ?> entity = getEntityUltimatelyReferredToBy(property);

		return entity == null //
				? property.getTypeInformation().getRequiredActualType() //
				: entity.getTypeInformation();
	}

	/**
	 * Returns the {@link PersistentEntity} identified by the given type.
	 * 
	 * @param type
	 * @return
	 * @throws IllegalStateException if the entity cannot be detected uniquely as multiple ones might share the same
	 *           identifier.
	 */
	@Nullable
	private PersistentEntity<?, ?> getEntityIdentifiedBy(TypeInformation<?> type) {

		Collection<PersistentEntity<?, ?>> entities = contexts.stream() //
				.flatMap(it -> it.getPersistentEntities().stream()) //
				.map(it -> it.getIdProperty()) //
				.filter(it -> it != null && type.equals(it.getTypeInformation().getActualType())) //
				.map(it -> it.getOwner()) //
				.collect(Collectors.toList());

		if (entities.size() > 1) {

			String message = "Found multiple entities identified by " + type.getType() + ": ";
			message += entities.stream().map(it -> it.getType().getName()).collect(Collectors.joining(", "));
			message += "! Introduce dedciated unique identifier types or explicitly define the target type in @Reference!";

			throw new IllegalStateException(message);
		}

		return entities.isEmpty() ? null : entities.iterator().next();
	}
}
