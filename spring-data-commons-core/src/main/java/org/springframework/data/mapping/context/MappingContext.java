/* Copyright 2004-2005 the original author or authors.
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
import java.util.List;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.validation.Validator;

/**
 * <p>This interface defines the overall context including all known
 * PersistentEntity instances and methods to obtain instances on demand</p>
 * <p/>
 * <p>This interface is used internally to establish associations
 * between entities and also at runtime to obtain entities by name</p>
 * <p/>
 * <p>The generic type parameters T & R are used to specify the
 * mapped form of a class (example Table) and property (example Column) respectively.</p>
 * <p/>
 *
 * @author Graeme Rocher
 * @author Jon Brisbin
 * @author Oliver Gierke
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
	 * Returns all {@link PersistentProperty}s for the given path expression based on the given root {@link Class}. Path
	 * expression are dot separated, e.g. {@code person.firstname}.
	 * 
	 * @param <T> 
	 * @param type
	 * @param path
	 * @return
	 */
	<T> Iterable<P> getPersistentPropertyPath(Class<T> type, String path);

	/**
	 * Obtains a validator for the given entity
	 * TODO: Why do we need validators at the {@link MappingContext}?
	 *
	 * @param entity The entity
	 * @return A validator or null if none exists for the given entity
	 */
	List<Validator> getEntityValidators(E entity);
}
