/*
 * Copyright 2018-2021 the original author or authors.
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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Pair;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A factory implementation to create {@link PersistentPropertyPath} instances in various ways.
 *
 * @author Oliver Gierke
 * @since 2.1
 * @soundtrack Cypress Hill - Boom Biddy Bye Bye (Fugees Remix, Unreleased & Revamped)
 */
class PersistentPropertyPathFactory<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	private static final Predicate<PersistentProperty<? extends PersistentProperty<?>>> IS_ENTITY = PersistentProperty::isEntity;

	private final Map<TypeAndPath, PersistentPropertyPath<P>> propertyPaths = new ConcurrentReferenceHashMap<>();
	private final MappingContext<E, P> context;

	public PersistentPropertyPathFactory(MappingContext<E, P> context) {
		this.context = context;
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} for the given property path on the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public PersistentPropertyPath<P> from(Class<?> type, String propertyPath) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(propertyPath, "Property path must not be null!");

		return getPersistentPropertyPath(ClassTypeInformation.from(type), propertyPath);
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} for the given property path on the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public PersistentPropertyPath<P> from(TypeInformation<?> type, String propertyPath) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(propertyPath, "Property path must not be null!");

		return getPersistentPropertyPath(type, propertyPath);
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} for the given {@link PropertyPath}.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public PersistentPropertyPath<P> from(PropertyPath path) {

		Assert.notNull(path, "Property path must not be null!");

		return from(path.getOwningType(), path.toDotPath());
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} based on a given type and {@link Predicate} to select properties
	 * matching it.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyFilter must not be {@literal null}.
	 * @return
	 */
	public <T> PersistentPropertyPaths<T, P> from(Class<T> type, Predicate<? super P> propertyFilter) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(propertyFilter, "Property filter must not be null!");

		return from(ClassTypeInformation.from(type), propertyFilter);
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} based on a given type and {@link Predicate} to select properties
	 * matching it.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyFilter must not be {@literal null}.
	 * @param traversalGuard must not be {@literal null}.
	 * @return
	 */
	public <T> PersistentPropertyPaths<T, P> from(Class<T> type, Predicate<? super P> propertyFilter,
			Predicate<P> traversalGuard) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(propertyFilter, "Property filter must not be null!");
		Assert.notNull(traversalGuard, "Traversal guard must not be null!");

		return from(ClassTypeInformation.from(type), propertyFilter, traversalGuard);
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} based on a given type and {@link Predicate} to select properties
	 * matching it.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyFilter must not be {@literal null}.
	 * @return
	 */
	public <T> PersistentPropertyPaths<T, P> from(TypeInformation<T> type, Predicate<? super P> propertyFilter) {
		return from(type, propertyFilter, it -> !it.isAssociation());
	}

	/**
	 * Creates a new {@link PersistentPropertyPath} based on a given type and {@link Predicate} to select properties
	 * matching it.
	 *
	 * @param type must not be {@literal null}.
	 * @param propertyFilter must not be {@literal null}.
	 * @param traversalGuard must not be {@literal null}.
	 * @return
	 */
	public <T> PersistentPropertyPaths<T, P> from(TypeInformation<T> type, Predicate<? super P> propertyFilter,
			Predicate<P> traversalGuard) {

		Assert.notNull(type, "Type must not be null!");
		Assert.notNull(propertyFilter, "Property filter must not be null!");
		Assert.notNull(traversalGuard, "Traversal guard must not be null!");

		return DefaultPersistentPropertyPaths.of(type,
				from(type, propertyFilter, traversalGuard, DefaultPersistentPropertyPath.empty()));
	}

	private PersistentPropertyPath<P> getPersistentPropertyPath(TypeInformation<?> type, String propertyPath) {

		return propertyPaths.computeIfAbsent(TypeAndPath.of(type, propertyPath),
				it -> createPersistentPropertyPath(it.getPath(), it.getType()));
	}

	/**
	 * Creates a {@link PersistentPropertyPath} for the given parts and {@link TypeInformation}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private PersistentPropertyPath<P> createPersistentPropertyPath(String propertyPath, TypeInformation<?> type) {

		String trimmedPath = propertyPath.trim();

		List<String> parts = trimmedPath.isEmpty() //
				? Collections.emptyList() //
				: Arrays.asList(trimmedPath.split("\\."));

		DefaultPersistentPropertyPath<P> path = DefaultPersistentPropertyPath.empty();
		Iterator<String> iterator = parts.iterator();
		E current = context.getRequiredPersistentEntity(type);

		while (iterator.hasNext()) {

			String segment = iterator.next();
			final DefaultPersistentPropertyPath<P> currentPath = path;

			Pair<DefaultPersistentPropertyPath<P>, E> pair = getPair(path, iterator, segment, current);

			if (pair == null) {

				String source = StringUtils.collectionToDelimitedString(parts, ".");

				throw new InvalidPersistentPropertyPath(source, type, segment, currentPath);
			}

			path = pair.getFirst();
			current = pair.getSecond();
		}

		return path;
	}

	@Nullable
	private Pair<DefaultPersistentPropertyPath<P>, E> getPair(DefaultPersistentPropertyPath<P> path,
			Iterator<String> iterator, String segment, E entity) {

		P property = entity.getPersistentProperty(segment);

		if (property == null) {
			return null;
		}

		TypeInformation<?> type = property.getTypeInformation().getRequiredActualType();
		return Pair.of(path.append(property), iterator.hasNext() ? context.getRequiredPersistentEntity(type) : entity);
	}

	private <T> Collection<PersistentPropertyPath<P>> from(TypeInformation<T> type, Predicate<? super P> filter,
			Predicate<P> traversalGuard, DefaultPersistentPropertyPath<P> basePath) {

		TypeInformation<?> actualType = type.getActualType();

		if (actualType == null) {
			return Collections.emptyList();
		}

		E entity = context.getRequiredPersistentEntity(actualType);
		Set<PersistentPropertyPath<P>> properties = new HashSet<>();

		PropertyHandler<P> propertyTester = persistentProperty -> {

			TypeInformation<?> typeInformation = persistentProperty.getTypeInformation();
			TypeInformation<?> actualPropertyType = typeInformation.getActualType();

			if (basePath.containsPropertyOfType(actualPropertyType)) {
				return;
			}

			DefaultPersistentPropertyPath<P> currentPath = basePath.append(persistentProperty);

			if (filter.test(persistentProperty)) {
				properties.add(currentPath);
			}

			if (traversalGuard.and(IS_ENTITY).test(persistentProperty)) {
				properties.addAll(from(typeInformation, filter, traversalGuard, currentPath));
			}
		};

		entity.doWithProperties(propertyTester);

		AssociationHandler<P> handler = association -> propertyTester.doWithPersistentProperty(association.getInverse());
		entity.doWithAssociations(handler);

		return properties;
	}

	static final class TypeAndPath {

		private final TypeInformation<?> type;
		private final String path;

		private TypeAndPath(TypeInformation<?> type, String path) {
			this.type = type;
			this.path = path;
		}

		public static TypeAndPath of(TypeInformation<?> type, String path) {
			return new TypeAndPath(type, path);
		}

		public TypeInformation<?> getType() {
			return this.type;
		}

		public String getPath() {
			return this.path;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof TypeAndPath)) {
				return false;
			}

			TypeAndPath that = (TypeAndPath) o;

			if (!ObjectUtils.nullSafeEquals(type, that.type)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(path, that.path);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(type);
			result = 31 * result + ObjectUtils.nullSafeHashCode(path);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "PersistentPropertyPathFactory.TypeAndPath(type=" + this.getType() + ", path=" + this.getPath() + ")";
		}
	}

	static class DefaultPersistentPropertyPaths<T, P extends PersistentProperty<P>>
			implements PersistentPropertyPaths<T, P> {

		private static final Comparator<PersistentPropertyPath<? extends PersistentProperty<?>>> SHORTEST_PATH = Comparator
				.comparingInt(PersistentPropertyPath::getLength);

		private final TypeInformation<T> type;
		private final Iterable<PersistentPropertyPath<P>> paths;

		private DefaultPersistentPropertyPaths(TypeInformation<T> type, Iterable<PersistentPropertyPath<P>> paths) {
			this.type = type;
			this.paths = paths;
		}

		/**
		 * Creates a new {@link DefaultPersistentPropertyPaths} instance
		 *
		 * @param type
		 * @param paths
		 * @return
		 */
		static <T, P extends PersistentProperty<P>> PersistentPropertyPaths<T, P> of(TypeInformation<T> type,
				Collection<PersistentPropertyPath<P>> paths) {

			List<PersistentPropertyPath<P>> sorted = new ArrayList<>(paths);

			Collections.sort(sorted, SHORTEST_PATH.thenComparing(ShortestSegmentFirst.INSTANCE));

			return new DefaultPersistentPropertyPaths<>(type, sorted);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.PersistentPropertyPaths#getFirst()
		 */
		@Override
		public Optional<PersistentPropertyPath<P>> getFirst() {
			return isEmpty() ? Optional.empty() : Optional.of(iterator().next());
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.PersistentPropertyPaths#contains(java.lang.String)
		 */
		@Override
		public boolean contains(String path) {
			return contains(PropertyPath.from(path, type));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.PersistentPropertyPaths#contains(org.springframework.data.mapping.PropertyPath)
		 */
		@Override
		public boolean contains(PropertyPath path) {

			Assert.notNull(path, "PropertyPath must not be null!");

			if (!path.getOwningType().equals(type)) {
				return false;
			}

			String dotPath = path.toDotPath();

			return stream().anyMatch(it -> dotPath.equals(it.toDotPath()));
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<PersistentPropertyPath<P>> iterator() {
			return paths.iterator();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.PersistentPropertyPaths#dropPathIfSegmentMatches(java.util.function.Predicate)
		 */
		@Override
		public PersistentPropertyPaths<T, P> dropPathIfSegmentMatches(Predicate<? super P> predicate) {

			Assert.notNull(predicate, "Predicate must not be null!");

			List<PersistentPropertyPath<P>> paths = this.stream() //
					.filter(it -> !it.stream().anyMatch(predicate)) //
					.collect(Collectors.toList());

			return paths.equals(this.paths) ? this : new DefaultPersistentPropertyPaths<>(type, paths);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "PersistentPropertyPathFactory.DefaultPersistentPropertyPaths(type=" + this.type + ", paths=" + this.paths
					+ ")";
		}

		/**
		 * Simple {@link Comparator} to sort {@link PersistentPropertyPath} instances by their property segment's name
		 * length.
		 *
		 * @author Oliver Gierke
		 * @since 2.1
		 */
		private enum ShortestSegmentFirst
				implements Comparator<PersistentPropertyPath<? extends PersistentProperty<?>>> {

			INSTANCE;

			@Override
			@SuppressWarnings("null")
			public int compare(PersistentPropertyPath<?> left, PersistentPropertyPath<?> right) {

				Function<PersistentProperty<?>, Integer> mapper = it -> it.getName().length();

				Stream<Integer> leftNames = left.stream().map(mapper);
				Stream<Integer> rightNames = right.stream().map(mapper);

				return StreamUtils.zip(leftNames, rightNames, (l, r) -> l - r) //
						.filter(it -> it != 0) //
						.findFirst() //
						.orElse(0);
			}
		}
	}
}
