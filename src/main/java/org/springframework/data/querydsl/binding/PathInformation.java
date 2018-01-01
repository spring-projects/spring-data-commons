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

import java.beans.PropertyDescriptor;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.lang.Nullable;

import com.querydsl.core.types.Path;

/**
 * Internal abstraction of mapped paths.
 *
 * @author Oliver Gierke
 * @since 1.13
 */
interface PathInformation {

	/**
	 * The type of the leaf property.
	 *
	 * @return
	 */
	Class<?> getLeafType();

	/**
	 * The type of the leaf property's parent type.
	 *
	 * @return
	 */
	Class<?> getLeafParentType();

	/**
	 * The name of the leaf property.
	 *
	 * @return
	 */
	String getLeafProperty();

	/**
	 * The {@link PropertyDescriptor} for the leaf property.
	 *
	 * @return
	 */
	@Nullable
	PropertyDescriptor getLeafPropertyDescriptor();

	/**
	 * The dot separated representation of the current path.
	 *
	 * @return
	 */
	String toDotPath();

	/**
	 * Tries to reify a Querydsl {@link Path} from the given {@link PropertyPath} and base.
	 *
	 * @param path must not be {@literal null}.
	 * @param base can be {@literal null}.
	 * @return
	 */
	Path<?> reifyPath(EntityPathResolver resolver);
}
