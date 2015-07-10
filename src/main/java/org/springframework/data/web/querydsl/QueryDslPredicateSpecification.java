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
import java.util.Collections;
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
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateSpecification {

	private final Map<String, PathAndBuilder> pathSpecs;
	private final Map<Class<?>, PathAndBuilder> typeSpecs;
	private Set<String> whiteList;
	private final Set<String> blackList;

	public QueryDslPredicateSpecification() {
		this.pathSpecs = new LinkedHashMap<String, PathAndBuilder>();
		this.typeSpecs = new LinkedHashMap<Class<?>, PathAndBuilder>();
		this.whiteList = new HashSet<String>();
		this.blackList = new HashSet<String>();
	}

	public Map<String, PathAndBuilder> getSpecs() {
		return Collections.unmodifiableMap(this.pathSpecs);
	}

	protected void define(String path, QueryDslPredicateBuilder<? extends Path<?>> builder) {
		this.pathSpecs.put(path, new PathAndBuilder(null, builder));
	}

	protected <T> void define(Class<T> type, QueryDslPredicateBuilder<? extends Path<T>> builder) {
		this.typeSpecs.put(type, new PathAndBuilder(null, builder));
	}

	protected <T extends Path<?>> void define(T path, QueryDslPredicateBuilder<T> builder) {
		this.pathSpecs.put(extractPropertyPath(path), new PathAndBuilder(path, builder));
	}

	protected void exclude(String... properties) {
		this.blackList.addAll(Arrays.asList(properties));
	}

	protected void include(String... properties) {
		this.whiteList.addAll(Arrays.asList(properties));
	}

	public boolean hasSpecificsForPath(String path) {
		return pathSpecs.containsKey(path);
	}

	public boolean isPathVisible(PropertyPath path) {

		List<String> segments = Arrays.asList(path.toDotPath().split("\\."));

		for (int i = 1; i <= segments.size(); i++) {
			if (!isPathVisible(StringUtils.collectionToDelimitedString(segments.subList(0, i), "."))) {
				return false;
			}
		}

		return true;
	}

	private boolean isPathVisible(String path) {

		if (whiteList.isEmpty() && blackList.isEmpty()) {
			return true;
		}

		if (!whiteList.isEmpty() && !whiteList.contains(path)) {
			return false;
		}

		if (!blackList.isEmpty() && blackList.contains(path)) {
			return false;
		}

		return true;
	}

	/**
	 * Returns the {@link QueryDslPredicateBuilder} for the given {@link PropertyPath}. Prefers a path configured for the
	 * specific path but falls back to the builder registered for a given type.
	 * 
	 * @param path must not be {@literal null}.
	 * @return
	 */
	public QueryDslPredicateBuilder<? extends Path<?>> getBuilderForPath(PropertyPath path) {

		Assert.notNull(path, "PropertyPath must not be null!");

		PathAndBuilder pathAndBuilder = pathSpecs.get(path.toDotPath());

		if (pathAndBuilder != null) {
			return pathAndBuilder.getBuilder();
		}

		pathAndBuilder = typeSpecs.get(path.getLeafProperty().getType());

		return pathAndBuilder == null ? null : pathAndBuilder.getBuilder();
	}

	public Path<?> getPathForStringPath(String path) {

		PathAndBuilder pathAndBuilder = pathSpecs.get(path);
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

	class PathAndBuilder {

		private final Path<?> path;
		private final QueryDslPredicateBuilder<? extends Path<?>> builder;

		public PathAndBuilder(Path<?> path, QueryDslPredicateBuilder<? extends Path<?>> builder) {
			this.path = path;
			this.builder = builder;
		}

		public Path<?> getPath() {
			return path;
		}

		public QueryDslPredicateBuilder<? extends Path<?>> getBuilder() {
			return builder;
		}

	}
}
