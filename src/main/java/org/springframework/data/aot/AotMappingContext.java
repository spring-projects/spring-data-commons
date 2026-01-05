/*
 * Copyright 2025-present the original author or authors.
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

import java.math.BigDecimal;
import java.text.Format;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Page;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.ClassGeneratingPropertyAccessorFactory;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiatorSource;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Simple {@link AbstractMappingContext} for processing of AOT contributions.
 *
 * @author Mark Paluch
 * @since 4.0
 */
class AotMappingContext extends
		AbstractMappingContext<BasicPersistentEntity<?, AotMappingContext.AotPersistentProperty>, AotMappingContext.AotPersistentProperty> {

	private final EntityInstantiators instantiators = new EntityInstantiators();
	private final AotAccessorFactory propertyAccessorFactory = new AotAccessorFactory();

	/**
	 * Contribute entity instantiators and property accessors for the given {@link PersistentEntity} that are captured
	 * through Spring's {@code CglibClassHandler}. Otherwise, this is a no-op if contributions are not ran through
	 * {@code CglibClassHandler}.
	 *
	 * @param entityType
	 */
	public void contribute(Class<?> entityType) {

		BasicPersistentEntity<?, AotPersistentProperty> entity = getPersistentEntity(entityType);

		if (entity != null) {

			EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
			if (instantiator instanceof EntityInstantiatorSource source) {
				source.getInstantiatorFor(entity);
			}

			propertyAccessorFactory.initialize(entity);
		}
	}

	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> typeInformation) {

		Class<?> type = typeInformation.getType();

		if (type.isArray() || type.isPrimitive() || type.isEnum()) {
			return false;
		}

		Predicate<Class<?>> isInPackage = packageMember -> type.getPackageName().startsWith(packageMember.getPackageName());

		if (isInPackage.test(UUID.class) // java.util
				|| isInPackage.test(BigDecimal.class) // java.math
				|| isInPackage.test(LocalDateTime.class) // java.time
				|| isInPackage.test(Format.class) // java.text
				|| isInPackage.test(Point.class) // org.springframework.data.geo
				|| isInPackage.test(Page.class) // org.springframework.data.domain
				|| type.getPackageName().startsWith("javax")) {
			return false;
		}

		return super.shouldCreatePersistentEntityFor(typeInformation);
	}

	@Override
	protected <T> BasicPersistentEntity<?, AotPersistentProperty> createPersistentEntity(
			TypeInformation<T> typeInformation) {
		return new BasicPersistentEntity<>(typeInformation);
	}

	@Override
	protected AotPersistentProperty createPersistentProperty(Property property,
			BasicPersistentEntity<?, AotPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		return new AotPersistentProperty(property, owner, simpleTypeHolder);
	}

	static class AotPersistentProperty extends AnnotationBasedPersistentProperty<AotPersistentProperty> {

		public AotPersistentProperty(Property property, PersistentEntity<?, AotPersistentProperty> owner,
				SimpleTypeHolder simpleTypeHolder) {
			super(property, owner, simpleTypeHolder);
		}

		@Override
		public boolean isAssociation() {
			return false;
		}

		@Override
		protected Association<AotPersistentProperty> createAssociation() {
			return new Association<>(this, null);
		}

		@Override
		public Association<AotPersistentProperty> getAssociation() {
			return new Association<>(this, null);
		}

	}

	static class AotAccessorFactory extends ClassGeneratingPropertyAccessorFactory {

		public void initialize(PersistentEntity<?, ?> entity) {
			if (isSupported(entity)) {
				potentiallyCreateAndRegisterPersistentPropertyAccessorClass(entity);
			}
		}
	}

}
