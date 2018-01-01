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
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * Factory to create {@link PersistentPropertyAccessor} for a given {@link PersistentEntity} and bean instance.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 1.13
 */
public interface PersistentPropertyAccessorFactory {

	/**
	 * Returns a {@link PersistentPropertyAccessor} for a given {@link PersistentEntity} and {@code bean}.
	 *
	 * @param entity must not be {@literal null}.
	 * @param bean must not be {@literal null}.
	 * @return will never be {@literal null}.
	 */
	PersistentPropertyAccessor getPropertyAccessor(PersistentEntity<?, ?> entity, Object bean);

	/**
	 * Returns whether given {@link PersistentEntity} is supported by this {@link PersistentPropertyAccessorFactory}.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	boolean isSupported(PersistentEntity<?, ?> entity);
}
