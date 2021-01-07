/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.util.function.Function;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.support.IsNewStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link IsNewStrategy} to use a {@link PersistentEntity}'s version property followed by it
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @soundtrack Scary Pockets - Crash Into Me (Dave Matthews Band Cover feat. Julia Nunes) -
 *             https://www.youtube.com/watch?v=syGlBNVGEqU
 */
class PersistentEntityIsNewStrategy implements IsNewStrategy {

	private final Function<Object, Object> valueLookup;
	private final @Nullable Class<?> valueType;

	/**
	 * Creates a new {@link PersistentEntityIsNewStrategy} for the given entity.
	 *
	 * @param entity must not be {@literal null}.
	 */
	private PersistentEntityIsNewStrategy(PersistentEntity<?, ?> entity, boolean idOnly) {

		Assert.notNull(entity, "PersistentEntity must not be null!");

		this.valueLookup = entity.hasVersionProperty() && !idOnly //
				? source -> entity.getPropertyAccessor(source).getProperty(entity.getRequiredVersionProperty())
				: source -> entity.getIdentifierAccessor(source).getIdentifier();

		this.valueType = entity.hasVersionProperty() && !idOnly //
				? entity.getRequiredVersionProperty().getType() //
				: entity.hasIdProperty() ? entity.getRequiredIdProperty().getType() : null;

		Class<?> type = valueType;

		if (type != null && type.isPrimitive()) {

			if (!ClassUtils.isAssignable(Number.class, type)) {

				throw new IllegalArgumentException(String
						.format("Only numeric primitives are supported as identifier / version field types! Got: %s.", valueType));
			}
		}
	}

	/**
	 * Creates a new {@link PersistentEntityIsNewStrategy} to only consider the identifier of the given entity.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public static PersistentEntityIsNewStrategy forIdOnly(PersistentEntity<?, ?> entity) {
		return new PersistentEntityIsNewStrategy(entity, true);
	}

	/**
	 * Creates a new {@link PersistentEntityIsNewStrategy} to consider version properties before falling back to the
	 * identifier.
	 *
	 * @param entity must not be {@literal null}.
	 * @return
	 */
	public static PersistentEntityIsNewStrategy of(PersistentEntity<?, ?> entity) {
		return new PersistentEntityIsNewStrategy(entity, false);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.IsNewStrategy#isNew(java.lang.Object)
	 */
	@Override
	public boolean isNew(Object entity) {

		Object value = valueLookup.apply(entity);

		if (value == null) {
			return true;
		}

		if (valueType != null && !valueType.isPrimitive()) {
			return false;
		}

		if (value instanceof Number) {
			return ((Number) value).longValue() == 0;
		}

		throw new IllegalArgumentException(
				String.format("Could not determine whether %s is new! Unsupported identifier or version property!", entity));
	}
}
