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

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Value object to access {@link PersistentEntity} instances managed by {@link MappingContext}s.
 *
 * @author Oliver Gierke
 * @since 1.8
 */
public class PersistentEntities implements Streamable<PersistentEntity<?, ? extends PersistentProperty<?>>> {

	private final Streamable<? extends MappingContext<?, ?>> contexts;

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
}
