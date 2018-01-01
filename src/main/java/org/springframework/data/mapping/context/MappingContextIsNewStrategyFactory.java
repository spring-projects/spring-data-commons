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
package org.springframework.data.mapping.context;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.function.Function;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.data.support.IsNewStrategyFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link IsNewStrategyFactory} using a {@link MappingContext} to determine the {@link IsNewStrategy} to be returned
 * for a particular type. It will look for a version and id property on the {@link PersistentEntity} and return a
 * strategy instance that will reflectively inspect the property for {@literal null} values or {@literal null} or a
 * value of 0 in case of a version property.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class MappingContextIsNewStrategyFactory extends IsNewStrategyFactorySupport {

	private final PersistentEntities context;

	/**
	 * Creates a new {@link MappingContextIsNewStrategyFactory} using the given {@link MappingContext}.
	 *
	 * @param context must not be {@literal null}.
	 * @deprecated use {@link MappingContextIsNewStrategyFactory(PersistentEntities)} instead.
	 */
	@Deprecated
	public MappingContextIsNewStrategyFactory(MappingContext<? extends PersistentEntity<?, ?>, ?> context) {
		this(new PersistentEntities(Collections.singletonList(context)));
	}

	/**
	 * Creates a new {@link MappingContextIsNewStrategyFactory} using the given {@link PersistentEntities}.
	 *
	 * @param entities must not be {@literal null}.
	 * @since 1.10
	 */
	public MappingContextIsNewStrategyFactory(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");
		this.context = entities;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.IsNewStrategyFactorySupport#getFallBackStrategy(java.lang.Class)
	 */
	@Nullable
	@Override
	protected IsNewStrategy doGetIsNewStrategy(Class<?> type) {

		PersistentEntity<?, ? extends PersistentProperty<?>> entity = context.getRequiredPersistentEntity(type);

		if (entity.hasVersionProperty()) {

			return PersistentPropertyInspectingIsNewStrategy.of(entity.getRequiredVersionProperty(),
					MappingContextIsNewStrategyFactory::propertyIsNullOrZeroNumber);

		}

		if (entity.hasIdProperty()) {

			return PersistentPropertyInspectingIsNewStrategy.of(entity.getRequiredIdProperty(),
					MappingContextIsNewStrategyFactory::propertyIsNull);
		}

		return null;
	}

	/**
	 * {@link IsNewStrategy} implementation that will inspect a given {@link PersistentProperty} and call
	 * {@link #isNew(Object)} with the value retrieved by reflection.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor(staticName = "of")
	static class PersistentPropertyInspectingIsNewStrategy implements IsNewStrategy {

		private final @NonNull PersistentProperty<?> property;
		private final @NonNull Function<Object, Boolean> isNew;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.support.IsNewStrategy#isNew(java.util.Optional)
		 */
		@Override
		public boolean isNew(Object entity) {

			Assert.notNull(entity, "Entity must not be null!");

			return isNew.apply(property.getOwner().getPropertyAccessor(entity).getProperty(property));
		}
	}

	private static boolean propertyIsNull(Object it) {
		return it == null;
	}

	private static boolean propertyIsNullOrZeroNumber(Object value) {
		return propertyIsNull(value) || value instanceof Number && ((Number) value).longValue() == 0;
	}
}
