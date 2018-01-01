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
 * PersistentPropertyAccessorFactory that uses a {@link BeanWrapper}.
 *
 * @author Oliver Gierke
 */
enum BeanWrapperPropertyAccessorFactory implements PersistentPropertyAccessorFactory {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.PersistentPropertyAccessorFactory#getPropertyAccessor(org.springframework.data.mapping.PersistentEntity, java.lang.Object)
	 */
	@Override
	public PersistentPropertyAccessor getPropertyAccessor(PersistentEntity<?, ?> entity, Object bean) {
		return new BeanWrapper<>(bean);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.PersistentPropertyAccessorFactory#isSupported(org.springframework.data.mapping.PersistentEntity)
	 */
	@Override
	public boolean isSupported(PersistentEntity<?, ?> entity) {
		return true;
	}
}
