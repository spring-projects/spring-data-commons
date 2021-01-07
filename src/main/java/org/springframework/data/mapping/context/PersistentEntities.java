/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.mapping.MappingException;
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
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.8
 */
public class PersistentEntities implements Streamable<PersistentEntity<?, ? extends PersistentProperty<?>>> {

	private final Collection<? extends MappingContext<?, ? extends PersistentProperty<?>>> contexts;

	/**
	 * Creates a new {@link PersistentEntities} for the given {@link MappingContext}s.
	 *
	 * @param contexts
	 */
	@SuppressWarnings("unchecked")
	public PersistentEntities(Iterable<? extends MappingContext<?, ?>> contexts) {

		Assert.notNull(contexts, "MappingContexts must not be null!");

		this.contexts = contexts instanceof Collection
				? (Collection<? extends MappingContext<?, ? extends PersistentProperty<?>>>) contexts
				: StreamSupport.stream(contexts.spliterator(), false).collect(Collectors.toList());
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
	 * @see MappingContext#hasPersistentEntityFor(Class)
	 * @see MappingContext#getPersistentEntity(Class)
	 */
	public Optional<PersistentEntity<?, ? extends PersistentProperty<?>>> getPersistentEntity(Class<?> type) {

		for (MappingContext<?, ? extends PersistentProperty<?>> context : contexts) {
			if (context.hasPersistentEntityFor(type)) {
				return Optional.of(context.getRequiredPersistentEntity(type));
			}
		}

		return Optional.empty();
	}

	/**
	 * Returns the {@link PersistentEntity} for the given type. Will consider all {@link MappingContext}s registered and
	 * create a new {@link PersistentEntity} in case none of the registered ones already have it registered for the given
	 * type, if there is only one context available.
	 *
	 * @param type must not be {@literal null}.
	 * @return the {@link PersistentEntity} for the given domain type.
	 * @throws org.springframework.data.mapping.MappingException if the {@link PersistentEntity} cannot be found or
	 *           {@link MappingContext#getPersistentEntity(Class) created} by the underlying {@link MappingContext}.
	 * @see MappingContext#getRequiredPersistentEntity(Class)
	 */
	public PersistentEntity<?, ? extends PersistentProperty<?>> getRequiredPersistentEntity(Class<?> type) {

		Assert.notNull(type, "Domain type must not be null!");

		if (contexts.size() == 1) {
			return contexts.iterator().next().getRequiredPersistentEntity(type);
		}

		return getPersistentEntity(type).orElseThrow(() -> {
			return new MappingException(String.format(
					"Cannot get or create PersistentEntity for type %s! PersistentEntities knows about %s MappingContext instances and therefore cannot identify a single responsible one. Please configure the initialEntitySet through an entity scan using the base package in your configuration to pre initialize contexts.",
					type.getName(), contexts.size()));
		});
	}

	/**
	 * Executes the given {@link BiFunction} on the given {@link MappingContext} and {@link PersistentEntity} based on the
	 * given type. Considers all {@link MappingContext}s for lookup. This method will create a new
	 * {@link PersistentEntity} in case there is only a single {@link MappingContext} registered.
	 *
	 * @param type must not be {@literal null}.
	 * @param combiner must not be {@literal null}.
	 * @return result of the {@link BiFunction}.
	 */
	public <T> Optional<T> mapOnContext(Class<?> type,
			BiFunction<MappingContext<?, ? extends PersistentProperty<?>>, PersistentEntity<?, ?>, T> combiner) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(combiner, "Combining BiFunction must not be null!");

		if (contexts.size() == 1) {
			return contexts.stream() //
					.filter(it -> it.getPersistentEntity(type) != null) //
					.map(it -> combiner.apply(it, it.getRequiredPersistentEntity(type))) //
					.findFirst();
		}

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

		Set<TypeInformation<?>> target = new HashSet<>();

		for (MappingContext<?, ? extends PersistentProperty<?>> context : contexts) {
			target.addAll(context.getManagedTypes());
		}

		return Streamable.of(target);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<PersistentEntity<?, ? extends PersistentProperty<?>>> iterator() {

		List<PersistentEntity<?, ? extends PersistentProperty<?>>> target = new ArrayList<>();

		for (MappingContext<?, ? extends PersistentProperty<?>> context : contexts) {
			target.addAll(context.getPersistentEntities());
		}

		return target.iterator();
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
				.map(PersistentEntity::getIdProperty) //
				.filter(it -> it != null && type.equals(it.getTypeInformation().getActualType())) //
				.map(PersistentProperty::getOwner) //
				.collect(Collectors.toList());

		if (entities.size() > 1) {

			String message = "Found multiple entities identified by " + type.getType() + ": ";
			message += entities.stream().map(it -> it.getType().getName()).collect(Collectors.joining(", "));
			message += "! Introduce dedicated unique identifier types or explicitly define the target type in @Reference!";

			throw new IllegalStateException(message);
		}

		return entities.isEmpty() ? null : entities.iterator().next();
	}
}
