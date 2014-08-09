/*
 * Copyright 2012 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * {@link IsNewStrategyFactory} that caches resolved {@link IsNewStrategy} instances per type to avoid re-resolving them
 * on each and every request.
 * 
 * @author Oliver Gierke
 */
public class CachingIsNewStrategyFactory implements IsNewStrategyFactory {

	private final Map<Class<?>, IsNewStrategy> cache = new ConcurrentHashMap<Class<?>, IsNewStrategy>();
	private final IsNewStrategyFactory delegate;

	/**
	 * Creates a new {@link CachingIsNewStrategyFactory} delegating to the given {@link IsNewStrategyFactory}.
	 * 
	 * @param delegate must not be {@literal null}.
	 */
	public CachingIsNewStrategyFactory(IsNewStrategyFactory delegate) {

		Assert.notNull(delegate, "IsNewStrategyFactory delegate must not be null!");
		this.delegate = delegate;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.IsNewStrategyFactory#getIsNewStrategy(java.lang.Class)
	 */
	public IsNewStrategy getIsNewStrategy(Class<?> type) {

		IsNewStrategy strategy = cache.get(type);

		if (strategy != null) {
			return strategy;
		}

		IsNewStrategy isNewStrategy = delegate.getIsNewStrategy(type);

		cache.put(type, isNewStrategy);
		return isNewStrategy;
	}
}
