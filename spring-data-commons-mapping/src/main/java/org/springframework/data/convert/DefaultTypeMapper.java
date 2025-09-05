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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.data.mapping.Alias;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link TypeMapper}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class DefaultTypeMapper<S> implements TypeMapper<S>, BeanClassLoaderAware {

	private final TypeAliasAccessor<S> accessor;
	private final List<? extends TypeInformationMapper> mappers;
	private final Map<Alias, Optional<TypeInformation<?>>> typeCache;

	private final Function<Alias, Optional<TypeInformation<?>>> getAlias;

	/**
	 * Creates a new {@link DefaultTypeMapper} using the given {@link TypeAliasAccessor}. It will use a
	 * {@link SimpleTypeInformationMapper} to calculate type aliases.
	 *
	 * @param accessor must not be {@literal null}.
	 */
	public DefaultTypeMapper(TypeAliasAccessor<S> accessor) {
		this(accessor, Collections.singletonList(new SimpleTypeInformationMapper()));
	}

	/**
	 * Creates a new {@link DefaultTypeMapper} using the given {@link TypeAliasAccessor} and {@link TypeInformationMapper}
	 * s.
	 *
	 * @param accessor must not be {@literal null}.
	 * @param mappers must not be {@literal null}.
	 */
	public DefaultTypeMapper(TypeAliasAccessor<S> accessor, List<? extends TypeInformationMapper> mappers) {
		this(accessor, null, mappers);
	}

	/**
	 * Creates a new {@link DefaultTypeMapper} using the given {@link TypeAliasAccessor}, {@link MappingContext} and
	 * additional {@link TypeInformationMapper}s. Will register a {@link MappingContextTypeInformationMapper} before the
	 * given additional mappers.
	 *
	 * @param accessor must not be {@literal null}.
	 * @param mappingContext
	 * @param additionalMappers must not be {@literal null}.
	 */
	public DefaultTypeMapper(TypeAliasAccessor<S> accessor,
			@Nullable MappingContext<? extends PersistentEntity<?, ?>, ?> mappingContext,
			List<? extends TypeInformationMapper> additionalMappers) {

		Assert.notNull(accessor, "Accessor must not be null");
		Assert.notNull(additionalMappers, "AdditionalMappers must not be null");

		List<TypeInformationMapper> mappers = new ArrayList<>(additionalMappers.size() + 1);
		if (mappingContext != null) {
			mappers.add(new MappingContextTypeInformationMapper(mappingContext));
		}
		mappers.addAll(additionalMappers);

		this.mappers = Collections.unmodifiableList(mappers);
		this.accessor = accessor;
		this.typeCache = new ConcurrentHashMap<>();
		this.getAlias = key -> {

			for (TypeInformationMapper mapper : mappers) {
				TypeInformation<?> typeInformation = mapper.resolveTypeFrom(key);

				if (typeInformation != null) {
					return Optional.of(typeInformation);
				}
			}
			return Optional.empty();
		};
	}

	@Override
	public @Nullable TypeInformation<?> readType(S source) {

		Assert.notNull(source, "Source object must not be null");

		return getFromCacheOrCreate(accessor.readAliasFrom(source));
	}

	/**
	 * Tries to lookup a {@link TypeInformation} for the given alias from the cache and return it if found. If none is
	 * found it'll consult the {@link TypeInformationMapper}s and cache the value found.
	 *
	 * @param alias
	 * @return
	 */
	private @Nullable TypeInformation<?> getFromCacheOrCreate(Alias alias) {
		return typeCache.computeIfAbsent(alias, getAlias).orElse(null);
	}

	@Override
	public <T> TypeInformation<? extends T> readType(S source, TypeInformation<T> basicType) {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(basicType, "Basic type must not be null");

		Class<?> documentsTargetType = getDefaultedTypeToBeUsed(source);

		if (documentsTargetType == null) {
			return basicType;
		}

		Class<T> rawType = basicType.getType();

		boolean isMoreConcreteCustomType = (rawType.isAssignableFrom(documentsTargetType)
				&& !rawType.equals(documentsTargetType));

		if (!isMoreConcreteCustomType) {
			return basicType;
		}

		TypeInformation<?> targetType = TypeInformation.of(documentsTargetType);

		return basicType.specialize(targetType);
	}

	/**
	 * Returns the type discovered through {@link #readType(Object)} but defaulted to the one returned by
	 * {@link #getFallbackTypeFor(Object)}.
	 *
	 * @param source
	 * @return
	 */
	private @Nullable Class<?> getDefaultedTypeToBeUsed(S source) {

		TypeInformation<?> type = readType(source);
		type = type == null ? getFallbackTypeFor(source) : type;
		return type == null ? null : type.getType();
	}

	/**
	 * Returns the type fallback {@link TypeInformation} in case none could be extracted from the given source.
	 *
	 * @param source will never be {@literal null}.
	 * @return
	 */
	protected @Nullable TypeInformation<?> getFallbackTypeFor(S source) {
		return null;
	}

	@Override
	public void writeType(Class<?> type, S dbObject) {
		writeType(TypeInformation.of(type), dbObject);
	}

	@Override
	public void writeType(TypeInformation<?> info, S sink) {

		Assert.notNull(info, "TypeInformation must not be null");

		Alias alias = getAliasFor(info);
		if (alias.isPresent()) {
			accessor.writeTypeTo(sink, alias.getRequiredValue());
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		for (TypeInformationMapper mapper : mappers) {
			if (mapper instanceof BeanClassLoaderAware) {
				((BeanClassLoaderAware) mapper).setBeanClassLoader(classLoader);
			}
		}
	}

	/**
	 * Returns the alias to be used for the given {@link TypeInformation}.
	 *
	 * @param info must not be {@literal null}
	 * @return the alias for the given {@link TypeInformation} or {@literal null} of none was found or all mappers
	 *         returned {@literal null}.
	 */
	protected final Alias getAliasFor(TypeInformation<?> info) {

		Assert.notNull(info, "TypeInformation must not be null");

		for (TypeInformationMapper mapper : mappers) {

			Alias alias = mapper.createAliasFor(info);
			if (alias.isPresent()) {
				return alias;
			}
		}

		return Alias.NONE;
	}
}
