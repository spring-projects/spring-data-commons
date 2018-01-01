/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.support;

import org.springframework.data.domain.Persistable;
import org.springframework.util.Assert;

/**
 * {@link IsNewStrategy} that invokes {@link Persistable#isNew()} on the given object.
 *
 * @author Oliver Gierke
 */
public enum PersistableIsNewStrategy implements IsNewStrategy {

	INSTANCE;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.IsNewStrategy#isNew(java.lang.Object)
	 */
	@Override
	public boolean isNew(Object entity) {

		Assert.notNull(entity, "Entity must not be null!");

		if (!(entity instanceof Persistable)) {
			throw new IllegalArgumentException(
					String.format("Given object of type %s does not implement %s!", entity.getClass(), Persistable.class));
		}

		return ((Persistable<?>) entity).isNew();
	}
}
