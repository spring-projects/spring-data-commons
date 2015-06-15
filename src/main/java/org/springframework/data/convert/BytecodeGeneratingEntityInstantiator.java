/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.convert;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.ParameterValueProvider;

/**
 * An {@link EntityInstantiator} that can generate byte code to speed-up dynamic object instantiation. Uses the
 * {@link PersistentEntity}'s {@link PreferredConstructor} to instantiate an instance of the entity by dynamically
 * generating factory methods with appropriate constructor invocations via ASM. If we cannot generate byte code for a
 * type, we gracefully fall-back to the {@link ReflectionEntityInstantiator}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.10
 * @deprecated since 1.11 in favor of {@link ClassGeneratingEntityInstantiator}.
 */
@Deprecated
public enum BytecodeGeneratingEntityInstantiator implements EntityInstantiator {

	INSTANCE;

	private final ClassGeneratingEntityInstantiator delegate = new ClassGeneratingEntityInstantiator();

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityInstantiator#createInstance(org.springframework.data.mapping.PersistentEntity, org.springframework.data.mapping.model.ParameterValueProvider)
	 */
	@Override
	public <T, E extends PersistentEntity<? extends T, P>, P extends PersistentProperty<P>> T createInstance(E entity,
			ParameterValueProvider<P> provider) {

		return this.delegate.createInstance(entity, provider);
	}

	/**
	 * @author Thomas Darimont
	 * @deprecated
	 */
	public interface ObjectInstantiator {

		Object newInstance(Object... args);
	}
}
