/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.domain;

import org.jspecify.annotations.Nullable;

/**
 * Simple interface for entities.
 * <p>
 * Note that methods declared in this interface ({@link #getId()} and {@link #isNew()}) become property accessors when
 * implementing this interface in combination with
 * {@link org.springframework.data.annotation.AccessType @AccessType(PROPERTY)}. Either of these can be marked as
 * transient when annotated with {@link org.springframework.data.annotation.Transient @Transient}.
 *
 * @param <ID> the type of the identifier
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public interface Persistable<ID> {

	/**
	 * Returns the id of the entity.
	 *
	 * @return the id. Can be {@literal null}.
	 */
	@Nullable
	ID getId();

	/**
	 * Returns if the {@code Persistable} is new or was persisted already.
	 *
	 * @return if {@literal true} the object is new.
	 */
	boolean isNew();
}
