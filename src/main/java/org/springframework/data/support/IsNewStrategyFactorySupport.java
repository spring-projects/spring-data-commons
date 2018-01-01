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
package org.springframework.data.support;

import org.springframework.data.domain.Persistable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link IsNewStrategyFactory} that handles {@link Persistable} implementations directly by returning a
 * {@link PersistableIsNewStrategy} and delegating to {@link #doGetIsNewStrategy(Class)} for all other types.
 *
 * @author Oliver Gierke
 * @since 1.5
 */
public abstract class IsNewStrategyFactorySupport implements IsNewStrategyFactory {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.support.IsNewStrategyFactory#getIsNewStrategy(java.lang.Class)
	 */
	public final IsNewStrategy getIsNewStrategy(Class<?> type) {

		Assert.notNull(type, "Type must not be null!");

		if (Persistable.class.isAssignableFrom(type)) {
			return PersistableIsNewStrategy.INSTANCE;
		}

		IsNewStrategy strategy = doGetIsNewStrategy(type);

		if (strategy != null) {
			return strategy;
		}

		throw new IllegalArgumentException(
				String.format("Unsupported entity %s! Could not determine IsNewStrategy.", type.getName()));
	}

	/**
	 * Determine the actual {@link IsNewStrategy} to be used for the given type.
	 *
	 * @param type will never be {@literal null}.
	 * @return the {@link IsNewStrategy} to be used for the given type or {@literal null} in case none can be resolved.
	 */
	@Nullable
	protected abstract IsNewStrategy doGetIsNewStrategy(Class<?> type);
}
