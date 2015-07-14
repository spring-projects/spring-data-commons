/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.web.querydsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.mysema.query.types.Path;

/**
 * {@link QuerydslBindings} allows definition of path specific {@link QuerydslBinding}. *
 * 
 * <pre>
 * <code>
 * new QuerydslBindings() {
 *   {
 *     bind(QUser.user.address.city, (path, value) -> path.like(value.toString()));
 *     bind(new StringPath("address.city"), (path, value) -> path.like(value.toString()));
 *   }
 * }
 * </code>
 * </pre>
 * 
 * @author Christoph Strobl
 * @since 1.11
 */
public class QuerydslBindings {

	private final Map<String, PathAndBinding> pathSpecs;
	private final Map<Class<?>, PathAndBinding> typeSpecs;
	private Set<String> whiteList;
	private final Set<String> blackList;

	public QuerydslBindings() {

		this.pathSpecs = new LinkedHashMap<String, PathAndBinding>();
		this.typeSpecs = new LinkedHashMap<Class<?>, PathAndBinding>();
		this.whiteList = new HashSet<String>();
		this.blackList = new HashSet<String>();
	}

	/**
	 * @param path
	 * @param builder
	 */
	protected QuerydslBindings bind(String path, QuerydslBinding<? extends Path<?>> builder) {

		Assert.hasText(path, "Cannot bind to null/empty path");
		this.pathSpecs.put(path, new PathAndBinding(null, builder));
		return this;
	}

	/**
	 * @param type
	 * @param builder
	 */
	protected <T> QuerydslBindings bind(Class<T> type, QuerydslBinding<? extends Path<T>> builder) {

		Assert.notNull(type, "Cannot bind to null type!");
		this.typeSpecs.put(type, new PathAndBinding(null, builder));
		return this;
	}

	/**
	 * @param path
	 * @param builder
	 */
	protected <T extends Path<?>> QuerydslBindings bind(T path, QuerydslBinding<T> builder) {

		Assert.notNull(path, "Cannot bind to null path!");
		this.pathSpecs.put(extractPropertyPath(path), new PathAndBinding(path, builder));
		return this;
	}

	/**
	 * Exclude properties from binding. <br />
	 * Exclusion of all properties of a nested type can be done by exclusion on a higher level. Eg. {@code address} would
	 * exclude both {@code address.city} and {@code address.street}.
	 * 
	 * @param properties
	 */
	protected QuerydslBindings excluding(String... properties) {
		this.blackList.addAll(Arrays.asList(properties));
		return this;
	}

	/**
	 * Include properties for binding. <br />
	 * Include the property considered a binding candidate.
	 * 
	 * @param properties
	 */
	protected QuerydslBindings including(String... properties) {
		this.whiteList.addAll(Arrays.asList(properties));
		return this;
	}

	/**
	 * Checks if a given {@link PropertyPath} should be visible for binding values.
	 * 
	 * @param path
	 * @return
	 */
	public boolean isPathVisible(PropertyPath path) {

		List<String> segments = Arrays.asList(path.toDotPath().split("\\."));

		for (int i = 1; i <= segments.size(); i++) {

			if (!isPathVisible(StringUtils.collectionToDelimitedString(segments.subList(0, i), "."))) {

				// check if full path is on whitelist if though partial one is not
				if (!whiteList.isEmpty()) {
					return whiteList.contains(path.toDotPath());
				}
				return false;
			}
		}

		return true;
	}

	private boolean isPathVisible(String path) {

		if (whiteList.isEmpty() && blackList.isEmpty()) {
			return true;
		}

		if (!blackList.isEmpty()) {

			if (blackList.contains(path)) {

				if (!whiteList.isEmpty() && whiteList.contains(path)) {
					return true;
				}
				return false;
			}

			return true;
		}

		if (!whiteList.isEmpty()) {

			if (whiteList.contains(path)) {
				return true;
			}
			return false;
		}

		return true;
	}

	/**
	 * Returns the {@link QuerydslBinding} for the given {@link PropertyPath}. Prefers a path configured for the specific
	 * path but falls back to the builder registered for a given type.
	 * 
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public QuerydslBinding<? extends Path<?>> getBindingForPath(PropertyPath path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		PathAndBinding pathAndBinding = pathSpecs.get(path.toDotPath());

		if (pathAndBinding != null) {
			return pathAndBinding.getBinding();
		}

		pathAndBinding = typeSpecs.get(path.getLeafProperty().getType());

		return pathAndBinding == null ? null : pathAndBinding.getBinding();
	}

	Path<?> getPath(PropertyPath path) {
		return getPathForStringPath(path.toDotPath());
	}

	private Path<?> getPathForStringPath(String path) {

		PathAndBinding pathAndBuilder = pathSpecs.get(path);
		if (pathAndBuilder == null) {
			return null;
		}

		return pathAndBuilder.getPath();
	}

	private String extractPropertyPath(Path<?> path) {

		if (path == null) {
			return "";
		}

		if (path.getMetadata().getParent() != null && !path.getMetadata().getParent().getMetadata().isRoot()) {
			return extractPropertyPath(path.getMetadata().getParent()) + "." + path.getMetadata().getName();
		}

		return path.getMetadata().getName();

	}

	/**
	 * @author Christoph Strobl
	 * @since 1.11
	 */
	private static class PathAndBinding {

		private final Path<?> path;
		private final QuerydslBinding<? extends Path<?>> binding;

		public PathAndBinding(Path<?> path, QuerydslBinding<? extends Path<?>> binding) {

			this.path = path;
			this.binding = binding;
		}

		public Path<?> getPath() {
			return path;
		}

		public QuerydslBinding<? extends Path<?>> getBinding() {
			return binding;
		}

	}
}
