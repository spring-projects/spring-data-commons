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
package org.springframework.data.convert;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link TypeInformationMapper} implementation that can be either set up using a {@link MappingContext} or manually set
 * up {@link Map} of {@link String} aliases to types. If a {@link MappingContext} is used the {@link Map} will be build
 * inspecting the {@link PersistentEntity} instances for type alias information.
 * 
 * @author Oliver Gierke
 */
public class MappingContextTypeInformationMapper implements TypeInformationMapper {

	private final Map<ClassTypeInformation<?>, Alias> typeMap;
	private final MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext;

	/**
	 * Creates a {@link MappingContextTypeInformationMapper} from the given {@link MappingContext}. Inspects all
	 * {@link PersistentEntity} instances for alias information and builds a {@link Map} of aliases to types from it.
	 * 
	 * @param mappingContext must not be {@literal null}.
	 */
	public MappingContextTypeInformationMapper(MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext) {

		Assert.notNull(mappingContext);

		this.typeMap = new ConcurrentHashMap<>();
		this.mappingContext = mappingContext;

		for (PersistentEntity<?, ?> entity : mappingContext.getPersistentEntities()) {
			verify(entity.getTypeInformation().getRawTypeInformation(), entity.getTypeAlias());
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.TypeInformationMapper#createAliasFor(org.springframework.data.util.TypeInformation)
	 */
	public Alias createAliasFor(TypeInformation<?> type) {

		return typeMap.computeIfAbsent(type.getRawTypeInformation(), key -> {
			return verify(key, mappingContext.getPersistentEntity(key).map(it -> it.getTypeAlias()).orElse(Alias.NONE));
		});
	}

	/**
	 * Adds the given alias to the cache in a {@literal null}-safe manner.
	 * 
	 * @param key must not be {@literal null}.
	 * @param alias can be {@literal null}.
	 */
	private Alias verify(ClassTypeInformation<?> key, Alias alias) {

		// Reject second alias for same type

		Alias existingAlias = typeMap.getOrDefault(key, Alias.NONE);

		if (existingAlias.isPresentButDifferent(alias)) {

			throw new IllegalArgumentException(
					String.format("Trying to register alias '%s', but found already registered alias '%s' for type %s!", alias,
							existingAlias, key));
		}

		// Reject second type for same alias

		if (typeMap.containsValue(alias)) {

			typeMap.entrySet().stream()//
					.filter(it -> it.getValue().hasSamePresentValueAs(alias) && !it.getKey().equals(key))//
					.findFirst().ifPresent(it -> {

						throw new IllegalArgumentException(String.format(
								"Detected existing type mapping of %s to alias '%s' but attempted to bind the same alias to %s!", key,
								alias, it.getKey()));
					});
		}

		return alias;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.TypeInformationMapper#resolveTypeFrom(java.util.Optional)
	 */
	@Override
	public Optional<TypeInformation<?>> resolveTypeFrom(Alias alias) {

		return alias.getValue().map(it -> {

			for (Entry<ClassTypeInformation<?>, Alias> entry : typeMap.entrySet()) {
				if (entry.getValue().hasValue(it)) {
					return entry.getKey();
				}
			}

			for (PersistentEntity<?, ?> entity : mappingContext.getPersistentEntities()) {

				if (entity.getTypeAlias().hasValue(it)) {
					return entity.getTypeInformation().getRawTypeInformation();
				}
			}

			return null;
		});
	}
}
