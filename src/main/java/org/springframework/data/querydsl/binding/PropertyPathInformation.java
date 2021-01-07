/*
 * Copyright 2016-2021 the original author or authors.
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
import java.util.Optional;

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
 * @since 1.13
 */
class PropertyPathInformation implements PathInformation {

	private final PropertyPath path;

	private PropertyPathInformation(PropertyPath path) {
		this.path = path;
	}

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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#getLeafType()
	 */
	@Override
	public Class<?> getLeafType() {
		return path.getLeafProperty().getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#getLeafParentType()
	 */
	@Override
	public Class<?> getLeafParentType() {
		return path.getLeafProperty().getOwningType().getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#getLeafProperty()
	 */
	@Override
	public String getLeafProperty() {
		return path.getLeafProperty().getSegment();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#getLeafPropertyDescriptor()
	 */
	@Nullable
	@Override
	public PropertyDescriptor getLeafPropertyDescriptor() {
		return BeanUtils.getPropertyDescriptor(getLeafParentType(), getLeafProperty());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#toDotPath()
	 */
	@Override
	public String toDotPath() {
		return path.toDotPath();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#reifyPath(org.springframework.data.querydsl.EntityPathResolver)
	 */
	@Override
	public Path<?> reifyPath(EntityPathResolver resolver) {
		return reifyPath(resolver, path, Optional.empty());
	}

	private static Path<?> reifyPath(EntityPathResolver resolver, PropertyPath path, Optional<Path<?>> base) {

		Optional<Path<?>> map = base.filter(it -> it instanceof CollectionPathBase).map(CollectionPathBase.class::cast)//
				.map(CollectionPathBase::any)//
				.map(Path.class::cast)//
				.map(it -> reifyPath(resolver, path, Optional.of(it)));

		return map.orElseGet(() -> {

			Path<?> entityPath = base.orElseGet(() -> resolver.createPath(path.getOwningType().getType()));

			Field field = org.springframework.data.util.ReflectionUtils.findRequiredField(entityPath.getClass(),
					path.getSegment());
			Object value = ReflectionUtils.getField(field, entityPath);

			PropertyPath next = path.next();

			if (next != null) {
				return reifyPath(resolver, next, Optional.of((Path<?>) value));
			}

			return (Path<?>) value;
		});
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

		if (!(o instanceof PropertyPathInformation)) {
			return false;
		}

		PropertyPathInformation that = (PropertyPathInformation) o;
		return ObjectUtils.nullSafeEquals(path, that.path);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(path);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PropertyPathInformation(path=" + this.path + ")";
	}
}
