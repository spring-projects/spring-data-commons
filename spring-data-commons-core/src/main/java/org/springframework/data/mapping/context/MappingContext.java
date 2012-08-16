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
package org.springframework.data.mapping.context;

import java.util.Collection;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.TypeInformation;

/**
 * This interface defines the overall context including all known PersistentEntity instances and methods to obtain
 * instances on demand. it is used internally to establish associations between entities and also at runtime to obtain
 * entities by name.
 * 
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Graeme Rocher
 */
public interface MappingContext<E extends PersistentEntity<?, P>, P extends PersistentProperty<P>> {

	/**
	 * Returns all {@link PersistentEntity}s held in the context.
	 * 
	 * @return
	 */
	Collection<E> getPersistentEntities();

	/**
	 * Returns a {@link PersistentEntity} for the given {@link Class}.
	 * 
	 * @param type
	 * @return
	 */
	E getPersistentEntity(Class<?> type);

	/**
	 * Returns a {@link PersistentEntity} for the given {@link TypeInformation}.
	 * 
	 * @param type
	 * @return
	 */
	E getPersistentEntity(TypeInformation<?> type);

	/**
	 * Returns the {@link PersistentEntity} mapped by the given {@link PersistentProperty}.
	 * 
	 * @param persistentProperty
	 * @return the {@link PersistentEntity} mapped by the given {@link PersistentProperty} or null if no
	 *         {@link PersistentEntity} exists for it or the {@link PersistentProperty} does not refer to an entity.
	 */
	E getPersistentEntity(P persistentProperty);

	/**
	 * Returns all {@link PersistentProperty}s for the given path expression based on the given {@link PropertyPath}.
	 * 
	 * @param <T>
	 * @param type
	 * @param path
	 * @return
	 */
	PersistentPropertyPath<P> getPersistentPropertyPath(PropertyPath propertyPath);
}
