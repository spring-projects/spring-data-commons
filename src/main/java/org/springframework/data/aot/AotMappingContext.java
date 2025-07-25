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
package org.springframework.data.aot;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.ClassGeneratingPropertyAccessorFactory;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.PersistentEntityClassInitializer;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Simple {@link AbstractMappingContext} for processing of AOT contributions.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class AotMappingContext extends
		AbstractMappingContext<BasicPersistentEntity<?, AotMappingContext.BasicPersistentProperty>, AotMappingContext.BasicPersistentProperty> {

	private final EntityInstantiators instantiators = new EntityInstantiators();
	private final ClassGeneratingPropertyAccessorFactory propertyAccessorFactory = new ClassGeneratingPropertyAccessorFactory();

	/**
	 * Contribute entity instantiators and property accessors for the given {@link PersistentEntity} that are captured
	 * through Spring's {@code CglibClassHandler}. Otherwise, this is a no-op if contributions are not ran through
	 * {@code CglibClassHandler}.
	 *
	 * @param entity
	 */
	public void contribute(PersistentEntity<?, ?> entity) {
		EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
		if (instantiator instanceof PersistentEntityClassInitializer pec) {
			pec.initialize(entity);
		}
		propertyAccessorFactory.initialize(entity);
	}

	@Override
	protected <T> BasicPersistentEntity<?, BasicPersistentProperty> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		return new BasicPersistentEntity<>(typeInformation);
	}

	@Override
	protected BasicPersistentProperty createPersistentProperty(Property property,
			BasicPersistentEntity<?, BasicPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		return new BasicPersistentProperty(property, owner, simpleTypeHolder);
	}

	static class BasicPersistentProperty extends AnnotationBasedPersistentProperty<BasicPersistentProperty> {

		public BasicPersistentProperty(Property property, PersistentEntity<?, BasicPersistentProperty> owner,
				SimpleTypeHolder simpleTypeHolder) {
			super(property, owner, simpleTypeHolder);
		}

		@Override
		protected Association<BasicPersistentProperty> createAssociation() {
			return null;
		}
	}

}
