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
package org.springframework.data.convert;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * {@link TypeInformationMapper} implementation that can be either set up using a {@link MappingContext} or manually set
 * up {@link Map} of {@link String} aliases to types. If a {@link MappingContext} is used the {@link Map} will be build
 * inspecting the {@link PersistentEntity} instances for type alias information.
 *
 * @author Oliver Gierke
 * @author Johannes Englmeier
 */
public class ConfigurableTypeInformationMapper implements TypeInformationMapper {

	private final Map<TypeInformation<?>, Alias> typeToAlias;
	private final Map<Alias, TypeInformation<?>> aliasToType;

	/**
	 * Creates a new {@link ConfigurableTypeInformationMapper} for the given type map.
	 *
	 * @param sourceTypeMap must not be {@literal null}.
	 */
	public ConfigurableTypeInformationMapper(Map<? extends Class<?>, String> sourceTypeMap) {

		Assert.notNull(sourceTypeMap, "SourceTypeMap must not be null");

		this.typeToAlias = new HashMap<>(sourceTypeMap.size());
		this.aliasToType = new HashMap<>(sourceTypeMap.size());

		for (Entry<? extends Class<?>, String> entry : sourceTypeMap.entrySet()) {

			TypeInformation<?> type = TypeInformation.of(entry.getKey());
			Alias alias = Alias.of(entry.getValue());

			if (typeToAlias.containsValue(alias)) {
				throw new IllegalArgumentException(
						String.format("Detected mapping ambiguity; String %s cannot be mapped to more than one type", alias));
			}

			this.typeToAlias.put(type, alias);
			this.aliasToType.put(alias, type);
		}
	}

	@Override
	public Alias createAliasFor(TypeInformation<?> type) {
		return typeToAlias.getOrDefault(type, Alias.NONE);
	}

	@Override
	public @Nullable TypeInformation<?> resolveTypeFrom(Alias alias) {
		return aliasToType.get(alias);
	}
}
