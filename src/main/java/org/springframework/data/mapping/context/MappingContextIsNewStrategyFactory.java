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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
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

		return context.getPersistentEntity(type)//
				.flatMap(it -> foo(it))//
				.orElseThrow(() -> new MappingException(String.format("Cannot determine IsNewStrategy for type %s!", type)));
	}

	private static Optional<IsNewStrategy> foo(PersistentEntity<?, ?> entity) {

		if (entity.hasVersionProperty()) {

			return entity.getVersionProperty().map(it -> PersistentPropertyInspectingIsNewStrategy.of(it,
					MappingContextIsNewStrategyFactory::propertyIsNullOrZeroNumber));

		} else if (entity.hasIdProperty()) {

			return entity.getIdProperty().map(
					it -> PersistentPropertyInspectingIsNewStrategy.of(it, MappingContextIsNewStrategyFactory::propertyIsNull));
		}

		return Optional.empty();
	}

	/**
	 * {@link IsNewStrategy} implementation that will inspect a given {@link PersistentProperty} and call
	 * {@link #decideIsNew(Object)} with the value retrieved by reflection.
	 * 
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor(staticName = "of")
	static class PersistentPropertyInspectingIsNewStrategy implements IsNewStrategy {

		private final @NonNull PersistentProperty<?> property;
		private final @NonNull Function<Optional<Object>, Boolean> isNew;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.support.IsNewStrategy#isNew(java.util.Optional)
		 */
		@Override
		public boolean isNew(Optional<? extends Object> entity) {

			return entity//
					.map(it -> isNew.apply(property.getOwner().getPropertyAccessor(it).getProperty(property)))//
					.orElse(false);
		}
	}

	private static boolean propertyIsNull(Optional<Object> it) {
		return !it.isPresent();
	}

	private static boolean propertyIsNullOrZeroNumber(Optional<Object> it) {

		return it.map(value -> {

			if (!(value instanceof Number)) {
				return false;
			}

			return ((Number) value).longValue() == 0;

		}).orElse(true);
	}
}
