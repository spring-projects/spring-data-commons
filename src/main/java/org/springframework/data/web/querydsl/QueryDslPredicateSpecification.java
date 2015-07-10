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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.mapping.PropertyPath;

import com.mysema.query.types.Path;

/**
 * @author Christoph Strobl
 * @since 1.11
 */
public class QueryDslPredicateSpecification {

	private final Map<String, PathAndBuilder> pathSpecs;
	private Set<String> whiteList;
	private final Set<String> blackList;

	public QueryDslPredicateSpecification() {
		this.pathSpecs = new LinkedHashMap<String, PathAndBuilder>();
		this.whiteList = new HashSet<String>();
		this.blackList = new HashSet<String>();
	}

	public Map<String, PathAndBuilder> getSpecs() {
		return Collections.unmodifiableMap(this.pathSpecs);
	}

	public void define(String path, QueryDslPredicateBuilder<? extends Path<?>> builder) {
		this.pathSpecs.put(path, new PathAndBuilder(null, builder));
	}

	public <T extends Path<?>> void define(T path, QueryDslPredicateBuilder<T> builder) {
		this.pathSpecs.put(extractPropetyPath(path), new PathAndBuilder(path, builder));
	}

	public void exclude(String... properties) {
		this.blackList.addAll(Arrays.asList(properties));
	}

	public boolean hasSpecificsForPath(String path) {
		return pathSpecs.containsKey(path);
	}

	public boolean isPathVisible(PropertyPath path) {

		Iterator<PropertyPath> it = path.iterator();
		String spath = "";
		while (it.hasNext()) {

			spath += it.next().getSegment();
			if (!isPathVisible(spath)) {
				return false;
			}
			if (it.hasNext()) {
				spath += ".";
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

	public QueryDslPredicateBuilder<? extends Path<?>> getBuilderForPath(String path) {

		PathAndBuilder pathAndBuilder = pathSpecs.get(path);
		if (pathAndBuilder == null) {
			return null;
		}

		return pathAndBuilder.getBuilder();
	}

	public Path<?> getPathForStringPath(String path) {

		PathAndBuilder pathAndBuilder = pathSpecs.get(path);
		if (pathAndBuilder == null) {
			return null;
		}

		return pathAndBuilder.getPath();
	}

	private String extractPropetyPath(Path<?> path) {

		if (path == null) {
			return "";
		}

		if (path.getMetadata().getParent() != null && !path.getMetadata().getParent().getMetadata().isRoot()) {
			return extractPropetyPath(path.getMetadata().getParent()) + "." + path.getMetadata().getName();
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
