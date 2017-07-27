/*
 * Copyright 2011-2017 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.lang.Nullable;

import com.mysema.commons.lang.Assert;

/**
 * Helper class to find a {@link PreferredConstructor}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Roman Rodov
 * @author Mark Paluch
 */
public interface PreferredConstructorDiscoverer<T, P extends PersistentProperty<P>> {

	/**
	 * Discovers the {@link PreferredConstructor} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 * @return the {@link PreferredConstructor} if found or {@literal null}.
	 */
	@Nullable
	static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(Class<T> type) {

		Assert.notNull(type, "Type must not be null!");

		return PreferredConstructorDiscoverers.findDiscoverer(type).discover(ClassTypeInformation.from(type), null);
	}

	/**
	 * Discovers the {@link PreferredConstructorDiscoverer} for the given {@link PersistentEntity}.
	 *
	 * @param entity must not be {@literal null}.
	 * @return the {@link PreferredConstructor} if found or {@literal null}.
	 */
	@Nullable
	static <T, P extends PersistentProperty<P>> PreferredConstructor<T, P> discover(PersistentEntity<T, P> entity) {

		Assert.notNull(entity, "PersistentEntity must not be null!");

		return PreferredConstructorDiscoverers.findDiscoverer(entity.getType()).discover(entity.getTypeInformation(),
				entity);
	}
}
