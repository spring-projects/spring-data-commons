/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;

/**
 * {@link QuerydslBindings} allows definition of path specific bindings.
 *
 * <pre>
 * <code>
 * new QuerydslBindings() {
 *   {
 *     bind(QUser.user.address.city).first((path, value) -> path.like(value.toString()));
 *     bind(String.class).first((path, value) -> path.like(value.toString()));
 *   }
 * }
 * </code>
 * </pre>
 *
 * The bindings can either handle a single - see {@link PathBinder#first(SingleValueBinding)} - (the first in case
 * multiple ones are supplied) or multiple - see {@link PathBinder#all(MultiValueBinding)} - value binding. If exactly
 * one path is deployed, an {@link AliasingPathBinder} is returned which - as the name suggests - allows aliasing of
 * paths, i.e. exposing the path under a different name.
 * <p>
 * {@link QuerydslBindings} are usually manipulated using a {@link QuerydslBinderCustomizer}, either implemented
 * directly or using a default method on a Spring Data repository.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.11
 * @see QuerydslBinderCustomizer
 */
public class QuerydslBindings {

	// pathSpecs key format: <class.simpleName>.<pathInformation.toDotPath>
	private final Map<String, PathAndBinding<?, ?>> pathSpecs;
	private final Map<Class<?>, PathAndBinding<?, ?>> typeSpecs;
	private final Set<String> allowList;
	private final Set<String> denyList;
	private final Set<String> aliases;

	private boolean excludeUnlistedProperties;

	/**
	 * Creates a new {@link QuerydslBindings} instance.
	 */
	public QuerydslBindings() {

		this.pathSpecs = new LinkedHashMap<>();
		this.typeSpecs = new LinkedHashMap<>();
		this.allowList = new HashSet<>();
		this.denyList = new HashSet<>();
		this.aliases = new HashSet<>();

	}

	/**
	 * Returns an {@link AliasingPathBinder} for the given {@link Path} to define bindings for them.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public final <T extends Path<S>, S> AliasingPathBinder<T, S> bind(T path) {
		return new AliasingPathBinder<>(path);
	}

	/**
	 * Returns a new {@link PathBinder} for the given {@link Path}s to define bindings for them.
	 *
	 * @param paths must not be {@literal null} or empty.
	 * @return
	 */
	@SafeVarargs
	public final <T extends Path<S>, S> PathBinder<T, S> bind(T... paths) {
		return new PathBinder<>(paths);
	}

	/**
	 * Returns a new {@link TypeBinder} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public final <T> TypeBinder<T> bind(Class<T> type) {
		return new TypeBinder<>(type);
	}

	/**
	 * Exclude properties from binding. Exclusion of all properties of a nested type can be done by exclusion on a higher
	 * level. E.g. {@code address} would exclude both {@code address.city} and {@code address.street}.
	 *
	 * @param paths must not be {@literal null} or empty.
	 */
	public final void excluding(Path<?>... paths) {

		Assert.notEmpty(paths, "At least one path has to be provided!");

		for (Path<?> path : paths) {
			this.denyList.add(toDotPath(Optional.of(path)));
		}
	}

	/**
	 * Include properties for binding. Include the property considered a binding candidate.
	 *
	 * @param paths must not be {@literal null} or empty.
	 */
	public final void including(Path<?>... paths) {

		Assert.notEmpty(paths, "At least one path has to be provided!");

		for (Path<?> path : paths) {
			this.allowList.add(toDotPath(Optional.of(path)));
		}
	}

	/**
	 * Returns whether to exclude all properties for which no explicit binding has been defined or it has been explicitly
	 * allowed. This defaults to {@literal false} which means that for properties without an explicitly defined binding a
	 * type specific default binding will be applied.
	 *
	 * @param excludeUnlistedProperties
	 * @return
	 * @see #including(Path...)
	 */
	public final QuerydslBindings excludeUnlistedProperties(boolean excludeUnlistedProperties) {

		this.excludeUnlistedProperties = excludeUnlistedProperties;
		return this;
	}

	/**
	 * Returns whether the given path is available on the given type.
	 *
	 * @param path must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	boolean isPathAvailable(String path, Class<?> type) {

		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(type, "Type must not be null!");

		return isPathAvailable(path, ClassTypeInformation.from(type));
	}

	/**
	 * Returns whether the given path is available on the given type.
	 *
	 * @param path must not be {@literal null}.
	 * @param type
	 * @return
	 */
	boolean isPathAvailable(String path, TypeInformation<?> type) {

		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(type, "Type must not be null!");

		return getPropertyPath(path, type) != null;
	}

	/**
	 * Returns the {@link SingleValueBinding} for the given {@link PropertyPath}. Prefers a path configured for the
	 * specific path but falls back to the builder registered for a given type.
	 *
	 * @param path must not be {@literal null}.
	 * @return can be {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	public <S extends Path<? extends T>, T> Optional<MultiValueBinding<S, T>> getBindingForPath(PathInformation path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		PathAndBinding<S, T> pathAndBinding = (PathAndBinding<S, T>) pathSpecs.get(createKey(path));

		if (pathAndBinding != null) {

			Optional<MultiValueBinding<S, T>> binding = pathAndBinding.getBinding();

			if (binding.isPresent()) {
				return binding;
			}
		}

		pathAndBinding = (PathAndBinding<S, T>) typeSpecs.get(path.getLeafType());

		return pathAndBinding == null ? Optional.empty() : pathAndBinding.getBinding();
	}

	/**
	 * Returns a {@link Path} for the {@link PropertyPath} instance.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	Optional<Path<?>> getExistingPath(PathInformation path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		return Optional.ofNullable(pathSpecs.get(createKey(path))).flatMap(PathAndBinding::getPath);
	}

	/**
	 * Returns the {@link PathInformation} for the given path and {@link TypeInformation}.
	 *
	 * @param path must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	@Nullable
	PathInformation getPropertyPath(String path, TypeInformation<?> type) {

		Assert.notNull(path, "Path must not be null!");
		Assert.notNull(type, "Type information must not be null!");

		if (!isPathVisible(path)) {
			return null;
		}

		// fully-qualified path lookup
		String key = createKey(type, path);
		if (pathSpecs.containsKey(key)) {
			return pathSpecs.get(key).getPath()//
					.map(QuerydslPathInformation::of)//
					.orElse(null);
		}

		// alias lookup
		if (pathSpecs.containsKey(path)) {
			return pathSpecs.get(path).getPath()//
					.map(QuerydslPathInformation::of)//
					.orElse(null);
		}

		try {
			PathInformation propertyPath = PropertyPathInformation.of(path, type);
			return isPathVisible(propertyPath) ? propertyPath : null;
		} catch (PropertyReferenceException o_O) {
			return null;
		}
	}

	/**
	 * Returns the property path key for the given {@link Path}.
	 *
	 * @param path can be {@literal null}.
	 * @return
	 */
	private static String createKey(Optional<Path<?>> path) {
		return path.map(QuerydslPathInformation::of).map(QuerydslBindings::createKey).orElse("");
	}

	private static String createKey(PathInformation path) {
		return createKey(path.getRootParentType(), path.toDotPath());
	}

	private static String createKey(TypeInformation<?> type, String path) {
		return createKey(type.getType(), path);
	}

	private static String createKey(Class<?> type, String path) {
		return type.getSimpleName() + "." + path;
	}

	/**
	 * Checks if a given {@link PropertyPath} should be visible for binding values.
	 *
	 * @param path
	 * @return
	 */
	private boolean isPathVisible(PathInformation path) {

		List<String> segments = Arrays.asList(path.toDotPath().split("\\."));

		for (int i = 1; i <= segments.size(); i++) {

			if (!isPathVisible(StringUtils.collectionToDelimitedString(segments.subList(0, i), "."))) {

				// check if full path is on allowList although the partial one is not
				if (!allowList.isEmpty()) {
					return allowList.contains(path.toDotPath());
				}

				return false;
			}
		}

		return true;
	}

	/**
	 * Returns whether the given path is visible, which means either an alias and not explicitly denied, explicitly
	 * allowed or not on the denylist if no allowlist configured.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private boolean isPathVisible(String path) {

		// Aliases are visible if not explicitly denied
		if (aliases.contains(path) && !denyList.contains(path)) {
			return true;
		}

		if (allowList.isEmpty()) {
			return excludeUnlistedProperties ? false : !denyList.contains(path);
		}

		return allowList.contains(path);
	}

	/**
	 * Returns the property path for the given {@link Path}.
	 *
	 * @param path can be {@literal null}.
	 * @return
	 */
	private static String toDotPath(Optional<Path<?>> path) {
		return path.map(QuerydslBindings::fromRootPath).orElse("");
	}

	private static String fromRootPath(Path<?> path) {

		Path<?> rootPath = path.getMetadata().getRootPath();

		if (rootPath == null) {
			throw new IllegalStateException(String.format("Couldn't find root path on path %s!", path));
		}

		return path.toString().substring(rootPath.getMetadata().getName().length() + 1);
	}

	/**
	 * A binder for {@link Path}s.
	 *
	 * @author Oliver Gierke
	 */
	public class PathBinder<P extends Path<? extends T>, T> {

		private final List<P> paths;

		/**
		 * Creates a new {@link PathBinder} for the given {@link Path}s.
		 *
		 * @param paths must not be {@literal null} or empty.
		 */
		@SafeVarargs
		PathBinder(P... paths) {

			Assert.notEmpty(paths, "At least one path has to be provided!");
			this.paths = Arrays.asList(paths);
		}

		/**
		 * Defines the given {@link SingleValueBinding} to be used for the paths.
		 *
		 * @param binding must not be {@literal null}.
		 */
		public void firstOptional(OptionalValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");

			all((path, value) -> binding.bind(path, Optionals.next(value.iterator())));
		}

		public void first(SingleValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");
			all((path, value) -> Optionals.next(value.iterator()).map(t -> binding.bind(path, t)));
		}

		/**
		 * Defines the given {@link MultiValueBinding} to be used for the paths.
		 *
		 * @param binding must not be {@literal null}.
		 */
		public void all(MultiValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");

			paths.forEach(path -> registerBinding(PathAndBinding.withPath(path).with(binding)));
		}

		protected void registerBinding(PathAndBinding<P, T> binding) {
			QuerydslBindings.this.pathSpecs.put(createKey(binding.getPath()), binding);
		}
	}

	/**
	 * A special {@link PathBinder} that additionally registers the binding under a dedicated alias. The original path is
	 * still registered but denied so that it becomes unavailable except it's explicitly allowed.
	 *
	 * @author Oliver Gierke
	 */
	public class AliasingPathBinder<P extends Path<? extends T>, T> extends PathBinder<P, T> {

		private final @Nullable String alias;
		private final P path;

		/**
		 * Creates a new {@link AliasingPathBinder} for the given {@link Path}.
		 *
		 * @param paths must not be {@literal null}.
		 */
		AliasingPathBinder(P path) {
			this(null, path);
		}

		/**
		 * Creates a new {@link AliasingPathBinder} using the given alias and {@link Path}.
		 *
		 * @param alias can be {@literal null}.
		 * @param path must not be {@literal null}.
		 */
		private AliasingPathBinder(@Nullable String alias, P path) {

			super(path);

			Assert.notNull(path, "Path must not be null!");

			this.alias = alias;
			this.path = path;
		}

		/**
		 * Aliases the current binding to be available under the given path. By default, the binding path will be denied so
		 * that aliasing effectively hides the original path. If you want to keep the original path around, include it in an
		 * explicit allowlist.
		 *
		 * @param alias must not be {@literal null}.
		 * @return will never be {@literal null}.
		 */
		public AliasingPathBinder<P, T> as(String alias) {

			Assert.hasText(alias, "Alias must not be null or empty!");
			return new AliasingPathBinder<>(alias, path);
		}

		/**
		 * Registers the current aliased binding to use the default binding.
		 */
		public void withDefaultBinding() {
			registerBinding(PathAndBinding.withPath(path));
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.querydsl.binding.QuerydslBindings.PathBinder#registerBinding(org.springframework.data.querydsl.binding.QuerydslBindings.PathAndBinding)
		 */
		@Override
		protected void registerBinding(PathAndBinding<P, T> binding) {

			super.registerBinding(binding);

			String dotPath = toDotPath(binding.getPath());

			if (alias != null) {
				QuerydslBindings.this.pathSpecs.put(alias, binding);
				QuerydslBindings.this.aliases.add(alias);
				QuerydslBindings.this.denyList.add(dotPath);
			}
		}
	}

	/**
	 * A binder for types.
	 *
	 * @author Oliver Gierke
	 */
	public final class TypeBinder<T> {

		private final Class<T> type;

		public TypeBinder(Class<T> type) {
			this.type = type;
		}

		/**
		 * Configures the given {@link SingleValueBinding} to be used for the current type.
		 *
		 * @param binding must not be {@literal null}.
		 */
		public <P extends Path<T>> void firstOptional(OptionalValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");
			all((MultiValueBinding<P, T>) (path, value) -> binding.bind(path, Optionals.next(value.iterator())));
		}

		public <P extends Path<T>> void first(SingleValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");
			all((MultiValueBinding<P, T>) (path, value) -> Optionals.next(value.iterator()).map(t -> binding.bind(path, t)));
		}

		/**
		 * Configures the given {@link MultiValueBinding} to be used for the current type.
		 *
		 * @param binding must not be {@literal null}.
		 */
		public <P extends Path<T>> void all(MultiValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");

			QuerydslBindings.this.typeSpecs.put(type, PathAndBinding.<T, P> withoutPath().with(binding));
		}
	}

	/**
	 * A pair of a {@link Path} and the registered {@link MultiValueBinding}.
	 *
	 * @author Christoph Strobl
	 * @author Oliver Gierke
	 * @since 1.11
	 */
	private static final class PathAndBinding<P extends Path<? extends T>, T> {

		private final Optional<Path<?>> path;
		private final Optional<MultiValueBinding<P, T>> binding;

		PathAndBinding(Optional<Path<?>> path, Optional<MultiValueBinding<P, T>> binding) {
			this.path = path;
			this.binding = binding;
		}

		public static <T, P extends Path<? extends T>> PathAndBinding<P, T> withPath(P path) {
			return new PathAndBinding<>(Optional.of(path), Optional.empty());
		}

		public static <T, S extends Path<? extends T>> PathAndBinding<S, T> withoutPath() {
			return new PathAndBinding<>(Optional.empty(), Optional.empty());
		}

		public PathAndBinding<P, T> with(MultiValueBinding<P, T> binding) {
			return new PathAndBinding<>(path, Optional.of(binding));
		}

		public Optional<Path<?>> getPath() {
			return this.path;
		}

		public Optional<MultiValueBinding<P, T>> getBinding() {
			return this.binding;
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
			if (!(o instanceof PathAndBinding)) {
				return false;
			}
			PathAndBinding<?, ?> that = (PathAndBinding<?, ?>) o;
			if (!ObjectUtils.nullSafeEquals(path, that.path)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(binding, that.binding);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(path);
			result = 31 * result + ObjectUtils.nullSafeHashCode(binding);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "QuerydslBindings.PathAndBinding(path=" + this.getPath() + ", binding=" + this.getBinding() + ")";
		}
	}
}
