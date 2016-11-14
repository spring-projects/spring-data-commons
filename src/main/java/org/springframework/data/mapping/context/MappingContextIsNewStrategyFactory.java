/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Arrays;
import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.data.support.IsNewStrategyFactorySupport;
import org.springframework.util.Assert;

/**
 * An {@link IsNewStrategyFactory} using a {@link MappingContext} to determine the {@link IsNewStrategy} to be returned
 * for a particular type. It will look for a version and id property on the {@link PersistentEntity} and return a
 * strategy instance that will refelctively inspect the property for {@literal null} values or {@literal null} or a
 * value of 0 in case of a version property.
 * 
 * @author Oliver Gierke
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
		this(new PersistentEntities(Arrays.asList(context)));
	}

	/**
	 * Creates a new {@link MappingContextIsNewStrategyFactory} using the given {@link PersistentEntities}.
	 * 
	 * @param context must not be {@literal null}.
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
	@Override
	protected IsNewStrategy doGetIsNewStrategy(Class<?> type) {

		PersistentEntity<?, ?> entity = context.getPersistentEntity(type);

		if (entity == null) {
			return null;
		}

		if (entity.hasVersionProperty()) {
			return new PropertyIsNullOrZeroNumberIsNewStrategy(entity.getVersionProperty().get());
		} else if (entity.hasIdProperty()) {
			return new PropertyIsNullIsNewStrategy(entity.getIdProperty().get());
		} else {
			throw new MappingException(String.format("Cannot determine IsNewStrategy for type %s!", type));
		}
	}

	/**
	 * {@link IsNewStrategy} implementation that will inspect a given {@link PersistentProperty} and call
	 * {@link #decideIsNew(Object)} with the value retrieved by reflection.
	 * 
	 * @author Oliver Gierke
	 */
	static abstract class PersistentPropertyInspectingIsNewStrategy implements IsNewStrategy {

		private final PersistentProperty<?> property;

		/**
		 * Creates a new {@link PersistentPropertyInspectingIsNewStrategy} using the given {@link PersistentProperty}.
		 * 
		 * @param property must not be {@literal null}.
		 */
		public PersistentPropertyInspectingIsNewStrategy(PersistentProperty<?> property) {
			Assert.notNull(property, "PersistentProperty must not be null!");
			this.property = property;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.support.IsNewStrategy#isNew(java.util.Optional)
		 */
		@Override
		public boolean isNew(Optional<? extends Object> entity) {

			return entity.map(it -> {

				PersistentPropertyAccessor accessor = property.getOwner().getPropertyAccessor(it);
				Object propertyValue = accessor.getProperty(property);

				return decideIsNew(propertyValue);

			}).orElse(false);
		}

		protected abstract boolean decideIsNew(Object property);
	}

	/**
	 * {@link IsNewStrategy} that does a check against {@literal null} for the given value and considers the object new if
	 * the value given is {@literal null}.
	 * 
	 * @author Oliver Gierke
	 */
	static class PropertyIsNullIsNewStrategy extends PersistentPropertyInspectingIsNewStrategy {

		/**
		 * Creates a new {@link PropertyIsNullIsNewStrategy} using the given {@link PersistentProperty}.
		 * 
		 * @param property must not be {@literal null}.
		 */
		public PropertyIsNullIsNewStrategy(PersistentProperty<?> property) {
			super(property);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.MappingContextIsNewStrategyFactory.PersistentPropertyInspectingIsNewStrategy#decideIsNew(java.lang.Object)
		 */
		@Override
		protected boolean decideIsNew(Object property) {
			return property == null;
		}
	}

	/**
	 * {@link IsNewStrategy} that considers property values of {@literal null} or 0 (in case of a {@link Number})
	 * implementation as indicators for the new state.
	 * 
	 * @author Oliver Gierke
	 */
	static class PropertyIsNullOrZeroNumberIsNewStrategy extends PersistentPropertyInspectingIsNewStrategy {

		/**
		 * Creates a new {@link PropertyIsNullOrZeroNumberIsNewStrategy} instance using the given {@link PersistentProperty}
		 * .
		 * 
		 * @param property must not be {@literal null}.
		 */
		public PropertyIsNullOrZeroNumberIsNewStrategy(PersistentProperty<?> property) {
			super(property);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.model.MappingContextIsNewStrategyFactory.PersistentPropertyInspectingIsNewStrategy#decideIsNew(java.lang.Object)
		 */
		@Override
		protected boolean decideIsNew(Object property) {

			if (property == null) {
				return true;
			}

			if (!(property instanceof Number)) {
				return false;
			}

			return ((Number) property).longValue() == 0;
		}
	}
}
