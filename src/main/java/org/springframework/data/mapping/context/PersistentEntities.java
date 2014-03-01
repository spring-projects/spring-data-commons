/*
 * Copyright 2014 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Value object to access {@link PersistentEntity} instances managed by {@link MappingContext}s.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class PersistentEntities implements Iterable<PersistentEntity<?, ?>> {

	private final Iterable<? extends MappingContext<?, ?>> contexts;

	/**
	 * Creates a new {@link PersistentEntities} for the given {@link MappingContext}s.
	 * 
	 * @param contexts
	 */
	public PersistentEntities(Iterable<? extends MappingContext<?, ?>> contexts) {

		Assert.notNull(contexts, "MappingContexts must not be null!");
		this.contexts = contexts;
	}

	/**
	 * Returns the {@link PersistentEntity} for the given type. Will consider all {@link MappingContext}s registered but
	 * return {@literal null} in case none of the registered ones already have a {@link PersistentEntity} registered for
	 * the given type.
	 * 
	 * @param type can be {@literal null}.
	 * @return
	 */
	public PersistentEntity<?, ?> getPersistentEntity(Class<?> type) {

		for (MappingContext<?, ?> context : contexts) {

			if (context.hasPersistentEntityFor(type)) {
				return context.getPersistentEntity(type);
			}
		}

		return null;
	}

	/**
	 * Returns all {@link TypeInformation} exposed by the registered {@link MappingContext}s.
	 * 
	 * @return
	 */
	public Iterable<TypeInformation<?>> getManagedTypes() {

		Set<TypeInformation<?>> informations = new HashSet<TypeInformation<?>>();

		for (MappingContext<?, ?> context : contexts) {
			informations.addAll(context.getManagedTypes());
		}

		return Collections.unmodifiableSet(informations);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<PersistentEntity<?, ?>> iterator() {

		List<PersistentEntity<?, ?>> entities = new ArrayList<PersistentEntity<?, ?>>();

		for (MappingContext<?, ?> context : contexts) {
			entities.addAll(context.getPersistentEntities());
		}

		return entities.iterator();
	}
}
