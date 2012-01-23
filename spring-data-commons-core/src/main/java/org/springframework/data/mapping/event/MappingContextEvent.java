/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.mapping.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
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

	private final TypeInformation<?> typeInformation;
	private final E source;

	/**
	 * Creates a new {@link MappingContextEvent} for the given {@link PersistentEntity}.
	 * 
	 * @param source must not be {@literal null}.
	 */
	@SuppressWarnings("deprecation")
	public MappingContextEvent(E source) {
		this(source, source.getTypeInformation());
	}

	/**
	 * Creates a new {@link MappingContextEvent} for the given {@link PersistentEntity} and {@link TypeInformation}.
	 * 
	 * @deprecated use {@link #MappingContextEvent(PersistentEntity)} instead. Will be removed for 1.3.GA.
	 * @param source must not be {@literal null}.
	 * @param typeInformation must not be {@literal null}.
	 */
	@Deprecated
	public MappingContextEvent(E source, TypeInformation<?> typeInformation) {
		super(source);

		Assert.notNull(source);
		Assert.notNull(typeInformation);

		this.source = source;
		this.typeInformation = typeInformation;
	}

	/**
	 * The {@link TypeInformation} the event was created for (usually the one derived from the underlying
	 * {@link PersistentEntity}).
	 * 
	 * @deprecated use the {@link TypeInformation} available from {@link #getPersistentEntity()} instead. Will be removed
	 *             for 1.3.GA.
	 * @return
	 */
	@Deprecated
	public TypeInformation<?> getTypeInformation() {
		return typeInformation;
	}

	/**
	 * Returns the {@link PersistentEntity} the event was created for.
	 * 
	 * @return
	 */
	public E getPersistentEntity() {
		return source;
	}
}
