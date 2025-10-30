/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.core;

import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Traversable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Bags;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Central API to expose information about custom collections present for Spring Data. Exposes custom collection and map
 * types and registers converters to convert them from and to Java-native collections.
 *
 * @author Oliver Drotbohm
 * @since 2.7
 * @soundtrack Black Sea Dahu - White Creatures (White Creatures)
 */
public class CustomCollections {

	private static final Set<Class<?>> CUSTOM_TYPES, CUSTOM_MAP_TYPES, CUSTOM_COLLECTION_TYPES, PAGINATION_RETURN_TYPES;
	private static final Set<Class<?>> COLLECTIONS_AND_MAP = Set.of(Collection.class, List.class, Set.class, Map.class);
	private static final SearchableTypes MAP_TYPES, COLLECTION_TYPES;
	private static final Collection<CustomCollectionRegistrar> REGISTRARS;

	static {

		CUSTOM_TYPES = new HashSet<>();
		PAGINATION_RETURN_TYPES = new HashSet<>();
		CUSTOM_MAP_TYPES = new HashSet<>();
		CUSTOM_COLLECTION_TYPES = new HashSet<>();

		REGISTRARS = SpringFactoriesLoader
				.loadFactories(CustomCollectionRegistrar.class, CustomCollections.class.getClassLoader())
				.stream()
				.filter(CustomCollectionRegistrar::isAvailable)
				.toList();

		REGISTRARS.forEach(it -> {

			it.getCollectionTypes().forEach(CustomCollections::registerCollectionType);
			it.getMapTypes().forEach(CustomCollections::registerMapType);
			it.getAllowedPaginationReturnTypes().forEach(PAGINATION_RETURN_TYPES::add);
		});

		MAP_TYPES = new SearchableTypes(CUSTOM_MAP_TYPES, Map.class);
		COLLECTION_TYPES = new SearchableTypes(CUSTOM_COLLECTION_TYPES, Collection.class);
	}

	/**
	 * Returns all custom collection and map types.
	 *
	 * @return will never be {@literal null}.
	 */
	public static Set<Class<?>> getCustomTypes() {
		return CUSTOM_TYPES;
	}

	/**
	 * Returns all types that are allowed pagination return types.
	 *
	 * @return will never be {@literal null}.
	 */
	public static Set<Class<?>> getPaginationReturnTypes() {
		return PAGINATION_RETURN_TYPES;
	}

	/**
	 * Returns whether the given type is a map base type.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static boolean isMapBaseType(Class<?> type) {

		Assert.notNull(type, "Type must not be null");

		return MAP_TYPES.has(type);
	}

	/**
	 * Returns the map base type for the given type, i.e. the one that's considered the logical map interface ({@link Map}
	 * for {@link HashMap} etc.).
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 * @throws IllegalArgumentException in case we do not find a map base type for the given one.
	 */
	public static Class<?> getMapBaseType(Class<?> type) throws IllegalArgumentException {
		return MAP_TYPES.getSuperType(type);
	}

	/**
	 * Returns whether the given type is considered a {@link Map} type.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static boolean isMap(Class<?> type) {
		return MAP_TYPES.hasSuperTypeFor(type);
	}

	/**
	 * Returns whether the given type is considered a {@link Collection} type.
	 *
	 * @param type must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	public static boolean isCollection(Class<?> type) {
		return COLLECTION_TYPES.hasSuperTypeFor(type);
	}

	/**
	 * Returns all unwrapper functions that transform the custom collections into Java-native ones.
	 *
	 * @return will never be {@literal null}.
	 */
	public static Set<Function<Object, Object>> getUnwrappers() {

		return REGISTRARS.stream()
				.map(CustomCollectionRegistrar::toJavaNativeCollection)
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Registers all converters to transform Java-native collections into custom ones and back in the given
	 * {@link ConverterRegistry}.
	 *
	 * @param registry must not be {@literal null}.
	 */
	public static void registerConvertersIn(ConverterRegistry registry) {

		Assert.notNull(registry, "ConverterRegistry must not be null");

		REGISTRARS.forEach(it -> it.registerConvertersIn(registry));
	}

	private static void registerCollectionType(Class<?> type) {

		CUSTOM_TYPES.add(type);
		CUSTOM_COLLECTION_TYPES.add(type);
	}

	private static void registerMapType(Class<?> type) {

		CUSTOM_TYPES.add(type);
		CUSTOM_MAP_TYPES.add(type);
	}

	private static class SearchableTypes {

		private static final BiPredicate<Class<?>, Class<?>> EQUALS = Object::equals;
		private static final BiPredicate<Class<?>, Class<?>> IS_ASSIGNABLE = Class::isAssignableFrom;
		private static final Function<Class<?>, Boolean> IS_NOT_NULL = Objects::nonNull;

		private final Collection<Class<?>> types;

		public SearchableTypes(Set<Class<?>> types, Class<?>... additional) {

			var all = new ArrayList<>(List.of(additional));
			all.addAll(types);

			this.types = all;
		}

		public boolean hasSuperTypeFor(Class<?> type) {

			Assert.notNull(type, "Type must not be null");

			return isOneOf(type, IS_ASSIGNABLE, IS_NOT_NULL);
		}

		/**
		 * Returns whether the current's raw type is one of the given ones.
		 *
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public boolean has(Class<?> type) {

			Assert.notNull(type, "Type must not be null");

			return isOneOf(type, EQUALS, IS_NOT_NULL);
		}

		/**
		 * Returns the super type of the given one from the set of types.
		 *
		 * @param type must not be {@literal null}.
		 * @return will never be {@literal null}.
		 * @throws IllegalArgumentException in case no base type of the given one can be found.
		 */
		public Class<?> getSuperType(Class<?> type) {

			Assert.notNull(type, "Type must not be null");

			Supplier<String> message = () -> String.format("Type %s not contained in candidates %s", type, types);

			return isOneOf(type, Class::isAssignableFrom, rejectNull(message));
		}

		/**
		 * Returns whether the given type matches one of the given candidates given the matcher with the
		 *
		 * @param <T>
		 * @param type the type to match against the current candidates.
		 * @param matcher how to match the candidates against the given type.
		 * @param resultMapper a {@link Function} to map the potentially given type to the actual result.
		 * @return will never be {@literal null}.
		 */
		private <T> T isOneOf(Class<?> type, BiPredicate<Class<?>, Class<?>> matcher, Function<Class<?>, T> resultMapper) {

			for (var candidate : types) {
				if (matcher.test(candidate, type)) {
					return resultMapper.apply(candidate);
				}
			}

			return resultMapper.apply(null);
		}

		/**
		 * Returns a function that rejects the source {@link Class} resolving the given message if the former is
		 * {@literal null}.
		 *
		 * @param message must not be {@literal null}.
		 * @return will never be {@literal null}.
		 */
		private static Function<Class<?>, Class<?>> rejectNull(Supplier<String> message) {

			Assert.notNull(message, "Message must not be null");

			return candidate -> {

				if (candidate == null) {
					throw new IllegalArgumentException(message.get());
				}

				return candidate;
			};

		}

	}

	static class VavrCollections implements CustomCollectionRegistrar {

		private static final TypeDescriptor OBJECT_DESCRIPTOR = TypeDescriptor.valueOf(Object.class);

		@Override
		public boolean isAvailable() {
			return ClassUtils.isPresent("io.vavr.control.Option", VavrCollections.class.getClassLoader());
		}

		@Override
		public Collection<Class<?>> getMapTypes() {
			return Set.of(io.vavr.collection.Map.class);
		}

		@Override
		public Collection<Class<?>> getCollectionTypes() {
			return List.of(Seq.class, io.vavr.collection.Set.class);
		}

		@Override
		public Collection<Class<?>> getAllowedPaginationReturnTypes() {
			return Set.of(Seq.class);
		}

		@Override
		public void registerConvertersIn(ConverterRegistry registry) {

			registry.addConverter(JavaToVavrCollectionConverter.INSTANCE);
			registry.addConverter(VavrToJavaCollectionConverter.INSTANCE);
		}

		@Override
		@SuppressWarnings("null")
		public Function<Object, Object> toJavaNativeCollection() {

			return source -> source instanceof io.vavr.collection.Traversable
					? VavrToJavaCollectionConverter.INSTANCE.convert(source, TypeDescriptor.forObject(source), OBJECT_DESCRIPTOR)
					: source;
		}

		private enum VavrToJavaCollectionConverter implements ConditionalGenericConverter {

			INSTANCE;

			private static final TypeDescriptor TRAVERSAL_TYPE = TypeDescriptor.valueOf(Traversable.class);

			@NonNull
			@Override
			public Set<ConvertiblePair> getConvertibleTypes() {

				return COLLECTIONS_AND_MAP.stream()
						.map(it -> new ConvertiblePair(Traversable.class, it))
						.collect(Collectors.toSet());
			}

			@Override
			public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

				return sourceType.isAssignableTo(TRAVERSAL_TYPE)
						&& COLLECTIONS_AND_MAP.contains(targetType.getType());
			}

			@Contract("null, _, _ -> null; !null, _, _ -> !null")
			@Override
			public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

				if (source == null) {
					return null;
				}

				if (source instanceof io.vavr.collection.Seq) {
					return ((io.vavr.collection.Seq<?>) source).asJava();
				}

				if (source instanceof io.vavr.collection.Map) {
					return ((io.vavr.collection.Map<?, ?>) source).toJavaMap();
				}

				if (source instanceof io.vavr.collection.Set) {
					return ((io.vavr.collection.Set<?>) source).toJavaSet();
				}

				throw new IllegalArgumentException("Unsupported Vavr collection " + source.getClass());
			}

		}

		private enum JavaToVavrCollectionConverter implements ConditionalGenericConverter {

			INSTANCE;

			private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS;

			static {

				Set<ConvertiblePair> pairs = new HashSet<>();
				pairs.add(new ConvertiblePair(Collection.class, io.vavr.collection.Traversable.class));
				pairs.add(new ConvertiblePair(Map.class, io.vavr.collection.Traversable.class));

				CONVERTIBLE_PAIRS = Collections.unmodifiableSet(pairs);
			}


			@Override
			public java.util.@NonNull Set<ConvertiblePair> getConvertibleTypes() {
				return CONVERTIBLE_PAIRS;
			}

			@Override
			public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

				// Prevent collections to be mapped to maps
				if (sourceType.isCollection() && io.vavr.collection.Map.class.isAssignableFrom(targetType.getType())) {
					return false;
				}

				// Prevent maps to be mapped to collections
				if (sourceType.isMap() && !(io.vavr.collection.Map.class.isAssignableFrom(targetType.getType())
						|| targetType.getType().equals(Traversable.class))) {
					return false;
				}

				return true;
			}

			@Contract("null, _, _ -> null; !null, _, _ -> !null")
			@Override
			public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceDescriptor,
					TypeDescriptor targetDescriptor) {

				var targetType = targetDescriptor.getType();

				if (io.vavr.collection.Seq.class.isAssignableFrom(targetType)) {
					return io.vavr.collection.List.ofAll((Iterable<?>) source);
				}

				if (io.vavr.collection.Set.class.isAssignableFrom(targetType)) {
					return LinkedHashSet.ofAll((Iterable<?>) source);
				}

				if (io.vavr.collection.Map.class.isAssignableFrom(targetType)) {
					return LinkedHashMap.ofAll((Map<?, ?>) source);
				}

				// No dedicated type asked for, probably Traversable.
				// Try to stay as close to the source value.

				if (source instanceof List) {
					return io.vavr.collection.List.ofAll((Iterable<?>) source);
				}

				if (source instanceof Set) {
					return LinkedHashSet.ofAll((Iterable<?>) source);
				}

				if (source instanceof Map) {
					return LinkedHashMap.ofAll((Map<?, ?>) source);
				}

				return source;
			}

		}

	}

	static class EclipseCollections implements CustomCollectionRegistrar {

		@Override
		public boolean isAvailable() {
			return ClassUtils.isPresent("org.eclipse.collections.api.list.ImmutableList",
					EclipseCollections.class.getClassLoader());
		}

		@Override
		public Collection<Class<?>> getCollectionTypes() {
			return List.of(ImmutableList.class, ImmutableSet.class, ImmutableBag.class, MutableList.class, MutableSet.class,
					MutableBag.class);
		}

		@Override
		public Collection<Class<?>> getMapTypes() {
			return List.of(ImmutableMap.class, MutableMap.class);
		}

		@Override
		public Collection<Class<?>> getAllowedPaginationReturnTypes() {
			return List.of(ImmutableList.class, MutableList.class);
		}

		@Override
		public Function<Object, Object> toJavaNativeCollection() {

			return source -> source instanceof RichIterable
					? EclipseToJavaConverter.INSTANCE.convert(source)
					: source;
		}

		@Override
		public void registerConvertersIn(ConverterRegistry registry) {

			registry.addConverter(EclipseToJavaConverter.INSTANCE);
			registry.addConverter(JavaToEclipseConverter.INSTANCE);
		}

		enum EclipseToJavaConverter implements Converter<Object, Object>, ConditionalConverter {

			INSTANCE;

			private static final TypeDescriptor RICH_ITERABLE_DESCRIPTOR = TypeDescriptor.valueOf(RichIterable.class);

			@Override
			public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

				return sourceType.isAssignableTo(RICH_ITERABLE_DESCRIPTOR)
						&& COLLECTIONS_AND_MAP.contains(targetType.getType());
			}

			@Contract("null -> null; !null -> !null")
			@Override
			public @Nullable Object convert(@Nullable Object source) {

				if (source instanceof ImmutableList) {
					return ((ImmutableList<?>) source).toList();
				}

				if (source instanceof ImmutableBag) {
					return ((ImmutableBag<?>) source).toList();
				}

				if (source instanceof ImmutableSet) {
					return ((ImmutableSet<?>) source).toSet();
				}

				if (source instanceof ImmutableMap) {
					return ((ImmutableMap<?, ?>) source).toMap();
				}

				return source;
			}

		}

		enum JavaToEclipseConverter implements ConditionalGenericConverter {

			INSTANCE;

			private static final Set<ConvertiblePair> CONVERTIBLE_PAIRS;

			static {

				var pairs = new HashSet<ConvertiblePair>();
				pairs.add(new ConvertiblePair(Collection.class, RichIterable.class));

				pairs.add(new ConvertiblePair(Set.class, MutableSet.class));
				pairs.add(new ConvertiblePair(Set.class, MutableList.class));
				pairs.add(new ConvertiblePair(Set.class, ImmutableSet.class));
				pairs.add(new ConvertiblePair(Set.class, ImmutableList.class));

				pairs.add(new ConvertiblePair(List.class, MutableList.class));
				pairs.add(new ConvertiblePair(List.class, ImmutableList.class));

				pairs.add(new ConvertiblePair(Map.class, RichIterable.class));
				pairs.add(new ConvertiblePair(Map.class, MutableMap.class));
				pairs.add(new ConvertiblePair(Map.class, ImmutableMap.class));

				CONVERTIBLE_PAIRS = Collections.unmodifiableSet(pairs);
			}

			@Override
			public @NonNull Set<ConvertiblePair> getConvertibleTypes() {
				return CONVERTIBLE_PAIRS;
			}

			@Override
			public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {

				// Prevent collections to be mapped to maps
				if (sourceType.isCollection() && MapIterable.class.isAssignableFrom(targetType.getType())) {
					return false;
				}

				// Prevent maps to be mapped to collections
				if (sourceType.isMap() //
						&& !(MapIterable.class.isAssignableFrom(targetType.getType())
								|| targetType.getType().equals(RichIterable.class))) {
					return false;
				}

				return true;
			}

			@Contract("null, _, _ -> null; !null, _ , _ -> !null")
			@Override
			public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceDescriptor,
					TypeDescriptor targetDescriptor) {

				var targetType = targetDescriptor.getType();

				if (ImmutableList.class.isAssignableFrom(targetType)) {
					return Lists.immutable.ofAll((Iterable<?>) source);
				}

				if (ImmutableSet.class.isAssignableFrom(targetType)) {
					return Sets.immutable.ofAll((Iterable<?>) source);
				}

				if (ImmutableBag.class.isAssignableFrom(targetType)) {
					return Bags.immutable.ofAll((Iterable<?>) source);
				}

				if (ImmutableMap.class.isAssignableFrom(targetType)) {
					return Maps.immutable.ofAll((Map<?, ?>) source);
				}

				if (MutableList.class.isAssignableFrom(targetType)) {
					return Lists.mutable.ofAll((Iterable<?>) source);
				}

				if (MutableSet.class.isAssignableFrom(targetType)) {
					return Sets.mutable.ofAll((Iterable<?>) source);
				}

				if (MutableBag.class.isAssignableFrom(targetType)) {
					return Bags.mutable.ofAll((Iterable<?>) source);
				}

				if (MutableMap.class.isAssignableFrom(targetType)) {
					return Maps.mutable.ofMap((Map<?, ?>) source);
				}

				// No dedicated type asked for, probably RichIterable.
				// Try to stay as close to the source value.

				if (source instanceof List) {
					return Lists.mutable.ofAll((Iterable<?>) source);
				}

				if (source instanceof Set) {
					return Sets.mutable.ofAll((Iterable<?>) source);
				}

				if (source instanceof Map) {
					return Maps.mutable.ofMap((Map<?, ?>) source);
				}

				return source;
			}
		}

	}

}
