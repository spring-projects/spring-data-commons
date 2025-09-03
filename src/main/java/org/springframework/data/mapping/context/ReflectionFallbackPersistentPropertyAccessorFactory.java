/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.mapping.context;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.BeanWrapperPropertyAccessorFactory;
import org.springframework.data.mapping.model.ClassGeneratingPropertyAccessorFactory;
import org.springframework.data.mapping.model.PersistentPropertyAccessorFactory;

/**
 * {@link PersistentPropertyAccessorFactory} that uses {@link ClassGeneratingPropertyAccessorFactory} if
 * {@link ClassGeneratingPropertyAccessorFactory#isSupported(PersistentEntity) supported} and falls back to reflection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class ReflectionFallbackPersistentPropertyAccessorFactory implements PersistentPropertyAccessorFactory {

	private final ClassGeneratingPropertyAccessorFactory accessorFactory = new ClassGeneratingPropertyAccessorFactory();

	@Override
	public <T> PersistentPropertyAccessor<T> getPropertyAccessor(PersistentEntity<?, ?> entity, T bean) {

		if (accessorFactory.isSupported(entity)) {
			return accessorFactory.getPropertyAccessor(entity, bean);
		}

		return BeanWrapperPropertyAccessorFactory.INSTANCE.getPropertyAccessor(entity, bean);
	}

	@Override
	public boolean isSupported(PersistentEntity<?, ?> entity) {
		return true;
	}
}
