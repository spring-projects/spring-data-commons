/*
 * Copyright 2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.core.NativeDetector;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * Common {@link EntityInstantiators} capable of creating entity instances using constructors and factory methods.
 *
 * @author Mark Paluch
 * @since 3.0
 * @see org.springframework.data.mapping.FactoryMethod
 * @see org.springframework.data.mapping.PreferredConstructor
 */
public class CommonEntityInstantiator implements EntityInstantiator {

	private final EntityInstantiator fallback = new KotlinClassGeneratingEntityInstantiator();

	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		var entityCreator = entity.getEntityCreator();

		if (entityCreator instanceof FactoryMethod || entityCreator == null || NativeDetector.inNativeImage()) {
			return ReflectionEntityInstantiator.INSTANCE.createInstance(entity, provider);
		}

		return fallback.createInstance(entity, provider);
	}

}
