/*
 * Copyright 2016-2018 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeanUtils;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.lang.Nullable;

import com.querydsl.core.types.Path;

/**
 * {@link PathInformation} based on a Querydsl {@link Path}.
 *
 * @author Oliver Gierke
 * @since 1.13
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(staticName = "of")
class QuerydslPathInformation implements PathInformation {

	private final Path<?> path;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.MappedPath#getLeafType()
	 */
	@Override
	public Class<?> getLeafType() {
		return path.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.MappedPath#getLeafParentType()
	 */
	@Override
	public Class<?> getLeafParentType() {

		Path<?> parent = path.getMetadata().getParent();

		if (parent == null) {
			throw new IllegalStateException(String.format("Could not obtain metadata for parent node of %s!", path));
		}

		return parent.getType();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.MappedPath#getLeafProperty()
	 */
	@Override
	public String getLeafProperty() {
		return path.getMetadata().getElement().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.MappedPath#getLeafPropertyDescriptor()
	 */
	@Nullable
	@Override
	public PropertyDescriptor getLeafPropertyDescriptor() {
		return BeanUtils.getPropertyDescriptor(getLeafParentType(), getLeafProperty());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.MappedPath#toDotPath()
	 */
	@Override
	public String toDotPath() {
		return QuerydslUtils.toDotPath(path);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.querydsl.binding.PathInformation#reifyPath(org.springframework.data.querydsl.EntityPathResolver)
	 */
	public Path<?> reifyPath(EntityPathResolver resolver) {
		return path;
	}
}
