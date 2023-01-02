/*
 * Copyright 2016-2023 the original author or authors.
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
import java.lang.reflect.Field;

import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.CollectionPathBase;

/**
 * {@link PropertyPath} based implementation of {@link PathInformation}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.13
 */
record PropertyPathInformation(PropertyPath path) implements PathInformation {

	/**
	 * Creates a new {@link PropertyPathInformation} for the given path and type.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static PropertyPathInformation of(String path, Class<?> type) {
		return PropertyPathInformation.of(PropertyPath.from(path, type));
	}

	/**
	 * Creates a new {@link PropertyPathInformation} for the given path and {@link TypeInformation}.
	 *
	 * @param path must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static PropertyPathInformation of(String path, TypeInformation<?> type) {
		return PropertyPathInformation.of(PropertyPath.from(path, type));
	}

	private static PropertyPathInformation of(PropertyPath path) {
		return new PropertyPathInformation(path);
	}

	@Override
	public Class<?> getRootParentType() {
		return path.getOwningType().getType();
	}

	@Override
	public Class<?> getLeafType() {
		return path.getLeafProperty().getType();
	}

	@Override
	public Class<?> getLeafParentType() {
		return path.getLeafProperty().getOwningType().getType();
	}

	@Override
	public String getLeafProperty() {
		return path.getLeafProperty().getSegment();
	}

	@Nullable
	@Override
	public PropertyDescriptor getLeafPropertyDescriptor() {
		return BeanUtils.getPropertyDescriptor(getLeafParentType(), getLeafProperty());
	}

	@Override
	public String toDotPath() {
		return path.toDotPath();
	}

	@Override
	public Path<?> reifyPath(EntityPathResolver resolver) {
		return reifyPath(resolver, path, null);
	}

	@SuppressWarnings("ConstantConditions")
	private static Path<?> reifyPath(EntityPathResolver resolver, PropertyPath path, @Nullable Path<?> base) {

		if (base instanceof CollectionPathBase) {
			return reifyPath(resolver, path, (Path<?>) ((CollectionPathBase<?, ?, ?>) base).any());
		}

		Path<?> entityPath = base != null ? base : resolver.createPath(path.getOwningType().getType());

		Field field = ReflectionUtils.findField(entityPath.getClass(), path.getSegment());
		Object value = ReflectionUtils.getField(field, entityPath);

		if (path.hasNext()) {
			return reifyPath(resolver, path.next(), (Path<?>) value);
		}

		return (Path<?>) value;
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
		return "PropertyPathInformation(path=" + this.path + ")";
	}
}
