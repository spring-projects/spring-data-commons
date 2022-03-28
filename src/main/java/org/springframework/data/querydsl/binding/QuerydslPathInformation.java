/*
 * Copyright 2016-2022 the original author or authors.
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

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeanUtils;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import com.querydsl.core.types.Path;

/**
 * {@link PathInformation} based on a Querydsl {@link Path}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @since 1.13
 */
class QuerydslPathInformation implements PathInformation {

	private final Path<?> path;

	private QuerydslPathInformation(Path<?> path) {
		this.path = path;
	}

	public static QuerydslPathInformation of(Path<?> path) {
		return new QuerydslPathInformation(path);
	}

	@Override
	public Class<?> getRootParentType() {
		return path.getRoot().getType();
	}

	@Override
	public Class<?> getLeafType() {
		return path.getType();
	}

	@Override
	public Class<?> getLeafParentType() {

		Path<?> parent = path.getMetadata().getParent();

		if (parent == null) {
			throw new IllegalStateException(String.format("Could not obtain metadata for parent node of %s!", path));
		}

		return parent.getType();
	}

	@Override
	public String getLeafProperty() {
		return path.getMetadata().getElement().toString();
	}

	@Nullable
	@Override
	public PropertyDescriptor getLeafPropertyDescriptor() {
		return BeanUtils.getPropertyDescriptor(getLeafParentType(), getLeafProperty());
	}

	@Override
	public String toDotPath() {
		return QuerydslUtils.toDotPath(path);
	}

	public Path<?> reifyPath(EntityPathResolver resolver) {
		return path;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof PathInformation that)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(getRootParentType(), that.getRootParentType())
				&& ObjectUtils.nullSafeEquals(toDotPath(), that.toDotPath());
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(getRootParentType());
		result = 31 * result + ObjectUtils.nullSafeHashCode(toDotPath());
		return result;
	}

	@Override
	public String toString() {
		return "QuerydslPathInformation(path=" + this.path + ")";
	}
}
