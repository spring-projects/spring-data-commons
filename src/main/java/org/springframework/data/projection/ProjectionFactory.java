/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.projection;

import org.springframework.lang.Nullable;

/**
 * A factory to create projecting instances for other objects usually used to allow easy creation of representation
 * projections to define which properties of a domain objects shall be exported in which way.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
public interface ProjectionFactory {

	/**
	 * Creates a projection of the given type for the given source object. The individual mapping strategy is defined by
	 * the implementations.
	 *
	 * @param projectionType the type to create, must not be {@literal null}.
	 * @param source the object to create a projection for, must not be {@literal null}.
	 * @return
	 */
	<T> T createProjection(Class<T> projectionType, Object source);

	/**
	 * Creates a projection to the given type for the given nullable source.
	 *
	 * @param projectionType must not be {@literal null}.
	 * @param source can be {@literal null}.
	 * @return
	 */
	@Nullable
	default <T> T createNullableProjection(Class<T> projectionType, @Nullable Object source) {
		return source == null ? null : createProjection(projectionType, source);
	}

	/**
	 * Creates a projection instance for the given type.
	 *
	 * @param projectionType the type to create, must not be {@literal null}.
	 * @return
	 */
	<T> T createProjection(Class<T> projectionType);

	/**
	 * Returns the {@link ProjectionInformation} for the given projection type.
	 *
	 * @param projectionType must not be {@literal null}.
	 * @return
	 * @since 1.12
	 */
	ProjectionInformation getProjectionInformation(Class<?> projectionType);
}
