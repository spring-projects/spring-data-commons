/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

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
 * @since 1.11
 * @see QuerydslBinderCustomizer
 */
public class QuerydslBindings {

	private final Map<String, PathAndBinding<?, ?>> pathSpecs;
	private final Map<Class<?>, PathAndBinding<?, ?>> typeSpecs;
	private final Set<String> whiteList, blackList, aliases;

	private boolean excludeUnlistedProperties;

	/**
	 * Creates a new {@link QuerydslBindings} instance.
	 */
	public QuerydslBindings() {

		this.pathSpecs = new LinkedHashMap<String, PathAndBinding<?, ?>>();
		this.typeSpecs = new LinkedHashMap<Class<?>, PathAndBinding<?, ?>>();
		this.whiteList = new HashSet<String>();
		this.blackList = new HashSet<String>();
		this.aliases = new HashSet<String>();

	}

	/**
	 * Returns an {@link AliasingPathBinder} for the given {@link Path} to define bindings for them.
	 * 
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public final <T extends Path<S>, S> AliasingPathBinder<T, S> bind(T path) {
		return new AliasingPathBinder<T, S>(path);
	}

	/**
	 * Returns a new {@link PathBinder} for the given {@link Path}s to define bindings for them.
	 * 
	 * @param paths must not be {@literal null} or empty.
	 * @return
	 */
	public final <T extends Path<S>, S> PathBinder<T, S> bind(T... paths) {
		return new PathBinder<T, S>(paths);
	}

	/**
	 * Returns a new {@link TypeBinder} for the given type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public final <T> TypeBinder<T> bind(Class<T> type) {
		return new TypeBinder<T>(type);
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
			this.blackList.add(toDotPath(path));
		}
	}

	/**
	 * Include properties for binding. Include the property considered a binding candidate.
	 * 
	 * @param properties must not be {@literal null} or empty.
	 */
	public final void including(Path<?>... paths) {

		Assert.notEmpty(paths, "At least one path has to be provided!");

		for (Path<?> path : paths) {
			this.whiteList.add(toDotPath(path));
		}
	}

	/**
	 * Returns whether to exclude all properties for which no explicit binding has been defined or it has been explicitly
	 * white-listed. This defaults to {@literal false} which means that for properties without an explicitly defined
	 * binding a type specific default binding will be applied.
	 * 
	 * @param excludeUnlistedProperties
	 * @return
	 * @see #including(String...)
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
	public <S extends Path<? extends T>, T> MultiValueBinding<S, T> getBindingForPath(PropertyPath path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		PathAndBinding<S, T> pathAndBinding = (PathAndBinding<S, T>) pathSpecs.get(path.toDotPath());

		if (pathAndBinding != null) {

			MultiValueBinding<S, T> binding = pathAndBinding.getBinding();

			if (binding != null) {
				return pathAndBinding.getBinding();
			}
		}

		pathAndBinding = (PathAndBinding<S, T>) typeSpecs.get(path.getLeafProperty().getType());

		return pathAndBinding == null ? null : pathAndBinding.getBinding();
	}

	/**
	 * Returns a {@link Path} for the {@link PropertyPath} instance.
	 * 
	 * @param path must not be {@literal null}.
	 * @return
	 */
	Path<?> getExistingPath(PropertyPath path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		PathAndBinding<?, ?> pathAndBuilder = pathSpecs.get(path.toDotPath());
		return pathAndBuilder == null ? null : pathAndBuilder.getPath();
	}

	/**
	 * @param path
	 * @param type
	 * @return
	 */
	PropertyPath getPropertyPath(String path, TypeInformation<?> type) {

		Assert.notNull(path, "Path must not be null!");

		if (!isPathVisible(path)) {
			return null;
		}

		if (pathSpecs.containsKey(path)) {
			return PropertyPath.from(toDotPath(pathSpecs.get(path).getPath()), type);
		}

		try {
			PropertyPath propertyPath = PropertyPath.from(path, type);
			return isPathVisible(propertyPath) ? propertyPath : null;
		} catch (PropertyReferenceException o_O) {
			return null;
		}
	}

	/**
	 * Checks if a given {@link PropertyPath} should be visible for binding values.
	 * 
	 * @param path
	 * @return
	 */
	private boolean isPathVisible(PropertyPath path) {

		List<String> segments = Arrays.asList(path.toDotPath().split("\\."));

		for (int i = 1; i <= segments.size(); i++) {

			if (!isPathVisible(StringUtils.collectionToDelimitedString(segments.subList(0, i), "."))) {

				// check if full path is on whitelist although the partial one is not
				if (!whiteList.isEmpty()) {
					return whiteList.contains(path.toDotPath());
				}

				return false;
			}
		}

		return true;
	}

	/**
	 * Returns whether the given path is visible, which means either an alias and not explicitly blacklisted, explicitly
	 * white listed or not on the black list if no white list configured.
	 * 
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private boolean isPathVisible(String path) {

		// Aliases are visible if not explicitly blacklisted
		if (aliases.contains(path) && !blackList.contains(path)) {
			return true;
		}

		if (whiteList.isEmpty()) {
			return excludeUnlistedProperties ? false : !blackList.contains(path);
		}

		return whiteList.contains(path);
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
		PathBinder(P... paths) {

			Assert.notEmpty(paths, "At least one path has to be provided!");
			this.paths = Arrays.asList(paths);
		}

		/**
		 * Defines the given {@link SingleValueBinding} to be used for the paths.
		 * 
		 * @param binding must not be {@literal null}.
		 * @return
		 */
		public void first(SingleValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");

			all(new MultiValueBindingAdapter<P, T>(binding));
		}

		/**
		 * Defines the given {@link MultiValueBinding} to be used for the paths.
		 * 
		 * @param binding must not be {@literal null}.
		 * @return
		 */
		public void all(MultiValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");

			for (P path : paths) {
				registerBinding(new PathAndBinding<P, T>(path, binding));
			}
		}

		protected void registerBinding(PathAndBinding<P, T> binding) {
			QuerydslBindings.this.pathSpecs.put(toDotPath(binding.getPath()), binding);
		}
	}

	/**
	 * A special {@link PathBinder} that additionally registers the binding under a dedicated alias. The original path is
	 * still registered but blacklisted so that it becomes unavailable except it's explicitly whitelisted.
	 *
	 * @author Oliver Gierke
	 */
	public class AliasingPathBinder<P extends Path<? extends T>, T> extends PathBinder<P, T> {

		private final String alias;
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
		@SuppressWarnings("unchecked")
		private AliasingPathBinder(String alias, P path) {

			super(path);

			Assert.notNull(path, "Path must not be null!");

			this.alias = alias;
			this.path = path;
		}

		/**
		 * Aliases the current binding to be available under the given path. By default, the binding path will be
		 * blacklisted so that aliasing effectively hides the original path. If you want to keep the original path around,
		 * include it in an explicit whitelist.
		 * 
		 * @param alias must not be {@literal null}.
		 * @return will never be {@literal null}.
		 */
		public AliasingPathBinder<P, T> as(String alias) {

			Assert.hasText(alias, "Alias must not be null or empty!");
			return new AliasingPathBinder<P, T>(alias, path);
		}

		/**
		 * Registers the current aliased binding to use the default binding.
		 */
		public void withDefaultBinding() {
			registerBinding(new PathAndBinding<P, T>(path, null));
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.querydsl.binding.QuerydslBindings.PathBinder#registerBinding(org.springframework.data.querydsl.binding.QuerydslBindings.PathAndBinding)
		 */
		@Override
		protected void registerBinding(PathAndBinding<P, T> binding) {

			super.registerBinding(binding);

			if (alias != null) {
				QuerydslBindings.this.pathSpecs.put(alias, binding);
				QuerydslBindings.this.aliases.add(alias);
				QuerydslBindings.this.blackList.add(toDotPath(binding.getPath()));
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

		/**
		 * Creates a new {@link TypeBinder} for the given type.
		 * 
		 * @param type must not be {@literal null}.
		 */
		private TypeBinder(Class<T> type) {

			Assert.notNull(type, "Type must not be null!");

			this.type = type;
		}

		/**
		 * Configures the given {@link SingleValueBinding} to be used for the current type.
		 * 
		 * @param binding must not be {@literal null}.
		 */
		public <P extends Path<T>> void first(SingleValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");
			all(new MultiValueBindingAdapter<P, T>(binding));
		}

		/**
		 * Configures the given {@link MultiValueBinding} to be used for the current type.
		 * 
		 * @param binding must not be {@literal null}.
		 */
		public <P extends Path<T>> void all(MultiValueBinding<P, T> binding) {

			Assert.notNull(binding, "Binding must not be null!");
			QuerydslBindings.this.typeSpecs.put(type, new PathAndBinding<P, T>(null, binding));
		}
	}

	/**
	 * A pair of a {@link Path} and the registered {@link MultiValueBinding}.
	 * 
	 * @author Christoph Strobl
	 * @since 1.11
	 */
	private static class PathAndBinding<S extends Path<? extends T>, T> {

		private final Path<?> path;
		private final MultiValueBinding<S, T> binding;

		/**
		 * Creates a new {@link PathAndBinding} for the given {@link Path} and {@link MultiValueBinding}.
		 * 
		 * @param path must not be {@literal null}.
		 * @param binding must not be {@literal null}.
		 */
		public PathAndBinding(S path, MultiValueBinding<S, T> binding) {

			this.path = path;
			this.binding = binding;
		}

		public Path<?> getPath() {
			return path;
		}

		public MultiValueBinding<S, T> getBinding() {
			return binding;
		}
	}

	/**
	 * {@link MultiValueBinding} that forwards the first value of the collection values to the delegate
	 * {@link SingleValueBinding}.
	 * 
	 * @author Oliver Gierke
	 */
	static class MultiValueBindingAdapter<T extends Path<? extends S>, S> implements MultiValueBinding<T, S> {

		private final SingleValueBinding<T, S> delegate;

		/**
		 * Creates a new {@link MultiValueBindingAdapter} for the given {@link SingleValueBinding}.
		 * 
		 * @param delegate must not be {@literal null}.
		 */
		public MultiValueBindingAdapter(SingleValueBinding<T, S> delegate) {
			this.delegate = delegate;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.web.querydsl.MultiValueBinding#bind(com.mysema.query.types.Path, java.util.Collection)
		 */
		@Override
		public Predicate bind(T path, Collection<? extends S> value) {
			Iterator<? extends S> iterator = value.iterator();
			return delegate.bind(path, iterator.hasNext() ? iterator.next() : null);
		}
	}
}
