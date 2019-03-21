/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.model.ParameterValueProvider;

/**
 * SPI to abstract strategies to create instances for {@link PersistentEntity}s.
 *
 * @author Oliver Gierke
 */
public interface EntityInstantiator {

	/**
	 * Creates a new instance of the given entity using the given source to pull data from.
	 *
	 * @param entity will not be {@literal null}.
	 * @param provider will not be {@literal null}.
	 * @return
	 */
	<T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider);
}
