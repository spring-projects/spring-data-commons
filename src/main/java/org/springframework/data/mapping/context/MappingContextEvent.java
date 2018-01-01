/*
 * Copyright 2011-2018 the original author or authors.
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
package org.springframework.data.mapping.context;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * Base implementation of an {@link ApplicationEvent} refering to a {@link PersistentEntity}.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @param <E> the {@link PersistentEntity} the context was created for
 * @param <P> the {@link PersistentProperty} the {@link PersistentEntity} consists of
 */
public class MappingContextEvent<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> extends
		ApplicationEvent {

	private static final long serialVersionUID = 1336466833846092490L;

	private final MappingContext<?, ?> source;
	private final E entity;

	/**
	 * Creates a new {@link MappingContextEvent} for the given {@link MappingContext} and {@link PersistentEntity}.
	 *
	 * @param source must not be {@literal null}.
	 * @param entity must not be {@literal null}.
	 */
	public MappingContextEvent(MappingContext<?, ?> source, E entity) {

		super(source);

		Assert.notNull(source, "Source MappingContext must not be null!");
		Assert.notNull(entity, "Entity must not be null!");

		this.source = source;
		this.entity = entity;
	}

	/**
	 * Returns the {@link PersistentEntity} the event was created for.
	 *
	 * @return
	 */
	public E getPersistentEntity() {
		return entity;
	}

	/**
	 * Returns whether the {@link MappingContextEvent} was triggered by the given {@link MappingContext}.
	 *
	 * @param context the {@link MappingContext} that potentially created the event.
	 * @return
	 */
	public boolean wasEmittedBy(MappingContext<?, ?> context) {
		return this.source.equals(context);
	}
}
