/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.querydsl;

import lombok.experimental.UtilityClass;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathType;

/**
 * Utility class for Querydsl.
 *
 * @author Oliver Gierke
 */
@UtilityClass
public class QuerydslUtils {

	public static final boolean QUERY_DSL_PRESENT = org.springframework.util.ClassUtils
			.isPresent("com.querydsl.core.types.Predicate", QuerydslUtils.class.getClassLoader());

	/**
	 * Returns the property path for the given {@link Path}.
	 *
	 * @param path can be {@literal null}.
	 * @return
	 */
	public static String toDotPath(Path<?> path) {
		return toDotPath(path, "");
	}

	/**
	 * Recursively builds up the dot path for the given {@link Path} instance by walking up the individual segments until
	 * the root.
	 *
	 * @param path can be {@literal null}.
	 * @param tail must not be {@literal null}.
	 * @return
	 */
	private static String toDotPath(@Nullable Path<?> path, String tail) {

		if (path == null) {
			return tail;
		}

		PathMetadata metadata = path.getMetadata();
		Path<?> parent = metadata.getParent();

		if (parent == null) {
			return tail;
		}

		if (metadata.getPathType().equals(PathType.DELEGATE)) {
			return toDotPath(parent, tail);
		}

		Object element = metadata.getElement();

		if (element == null || !StringUtils.hasText(element.toString())) {
			return toDotPath(parent, tail);
		}

		return toDotPath(parent, StringUtils.hasText(tail) ? String.format("%s.%s", element, tail) : element.toString());
	}
}
