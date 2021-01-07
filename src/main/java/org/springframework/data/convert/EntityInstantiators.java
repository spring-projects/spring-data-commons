/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.convert;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.InternalEntityInstantiatorFactory;

/**
 * Simple value object allowing access to {@link EntityInstantiator} instances for a given type falling back to a
 * default one.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @deprecated since 2.3, use {@link org.springframework.data.mapping.model.EntityInstantiators} instead.
 */
@Deprecated
public class EntityInstantiators extends org.springframework.data.mapping.model.EntityInstantiators {

	/**
	 * Creates a new {@link EntityInstantiators} using the default fallback instantiator and no custom ones.
	 */
	public EntityInstantiators() {
		super();
	}

	/**
	 * Creates a new {@link EntityInstantiators} using the given {@link EntityInstantiator} as fallback.
	 *
	 * @param fallback must not be {@literal null}.
	 */
	public EntityInstantiators(EntityInstantiator fallback) {
		super(fallback, Collections.emptyMap());
	}

	/**
	 * Creates a new {@link EntityInstantiators} using the default fallback instantiator and the given custom ones.
	 *
	 * @param customInstantiators must not be {@literal null}.
	 */
	public EntityInstantiators(Map<Class<?>, EntityInstantiator> customInstantiators) {
		super(InternalEntityInstantiatorFactory.getKotlinClassGeneratingEntityInstantiator(),
				adaptFromLegacy(customInstantiators));
	}

	/**
	 * Creates a new {@link EntityInstantiator} using the given fallback {@link EntityInstantiator} and the given custom
	 * ones.
	 *
	 * @param defaultInstantiator must not be {@literal null}.
	 * @param customInstantiators must not be {@literal null}.
	 */
	public EntityInstantiators(EntityInstantiator defaultInstantiator,
			Map<Class<?>, EntityInstantiator> customInstantiators) {
		super(defaultInstantiator, adaptFromLegacy(customInstantiators));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.EntityInstantiators#getInstantiatorFor(org.springframework.data.mapping.PersistentEntity)
	 */
	@Override
	public EntityInstantiator getInstantiatorFor(PersistentEntity<?, ?> entity) {
		return new EntityInstantiatorAdapter(super.getInstantiatorFor(entity));
	}

	private static Map<Class<?>, org.springframework.data.mapping.model.EntityInstantiator> adaptFromLegacy(
			Map<Class<?>, EntityInstantiator> instantiators) {

		return instantiators == null //
				? null //
				: instantiators.entrySet().stream() //
						.collect(Collectors.toMap(Entry::getKey, e -> new EntityInstantiatorAdapter(e.getValue())));
	}
}
