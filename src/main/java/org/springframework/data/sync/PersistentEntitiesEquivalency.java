/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.sync;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.sync.diffsync.Equivalency;
import org.springframework.util.Assert;

/**
 * {@link Equivalency} implementation to inspect the Spring Data {@link PersistentEntities} for the mapping metadata of
 * the given types to llokup their exposed identifier properties. Using a Spring Data {@link BeanWrapper} to obtain the
 * actual identifier values for final comaprison.
 * 
 * @author Oliver Gierke
 * @since 1.10
 */
class PersistentEntitiesEquivalency implements Equivalency {

	private final PersistentEntities entities;

	/**
	 * Creates a new {@link PersistentEntitiesEquivalency} from the given {@link PersistentEntities}.
	 * 
	 * @param entities must not be {@literal null}.
	 */
	public PersistentEntitiesEquivalency(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		this.entities = entities;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.sync.diffsync.Equivalency#isEquivalent(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean isEquivalent(Object left, Object right) {

		if (left == null || right == null) {
			return false;
		}

		Object leftId = getIdProperty(left);
		Object rightId = getIdProperty(right);

		return leftId == null || rightId == null ? false : leftId.equals(rightId);
	}

	private Object getIdProperty(Object object) {

		PersistentEntity<?, ?> entity = entities.getPersistentEntity(object.getClass());
		return entity.getIdentifierAccessor(object).getIdentifier();
	}
}
