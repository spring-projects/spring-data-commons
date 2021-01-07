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
package org.springframework.data.auditing;

import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.util.Assert;

/**
 * {@link AuditingHandler} extension that uses {@link PersistentEntity#isNew(Object)} to expose a generic
 * {@link #markAudited(Optional)} method that will route calls to {@link #markCreated(Optional)} or
 * {@link #markModified(Optional)} based on the {@link IsNewStrategy} determined from the factory.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
public class IsNewAwareAuditingHandler extends AuditingHandler {

	private final PersistentEntities entities;

	/**
	 * Creates a new {@link IsNewAwareAuditingHandler} for the given {@link MappingContext}.
	 *
	 * @param mappingContext must not be {@literal null}.
	 * @since 1.8
	 * @deprecated use {@link IsNewAwareAuditingHandler(PersistentEntities)} instead.
	 */
	@Deprecated
	public IsNewAwareAuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(PersistentEntities.of(mappingContext));
	}

	/**
	 * Creates a new {@link IsNewAwareAuditingHandler} for the given {@link MappingContext}.
	 *
	 * @param entities must not be {@literal null}.
	 * @since 1.10
	 */
	public IsNewAwareAuditingHandler(PersistentEntities entities) {

		super(entities);

		this.entities = entities;
	}

	/**
	 * Marks the given object created or modified based on {@link PersistentEntity#isNew(Object)}. Will route the calls to
	 * {@link #markCreated(Optional)} and {@link #markModified(Optional)} accordingly.
	 *
	 * @param object must not be {@literal null}.
	 */
	public Object markAudited(Object object) {

		Assert.notNull(object, "Source object must not be null!");

		if (!isAuditable(object)) {
			return object;
		}

		PersistentEntity<?, ? extends PersistentProperty<?>> entity = entities
				.getRequiredPersistentEntity(object.getClass());

		return entity.isNew(object) ? markCreated(object) : markModified(object);
	}
}
