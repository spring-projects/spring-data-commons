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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link IsNewStrategyFactory} that caches resolved {@link IsNewStrategy} instances per type to avoid re-resolving them
 * on each and every request.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RequiredArgsConstructor
public class CachingIsNewStrategyFactory implements IsNewStrategyFactory {

	private final @NonNull IsNewStrategyFactory delegate;
	private final Map<Class<?>, IsNewStrategy> cache = new ConcurrentHashMap<>();

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.IsNewStrategyFactory#getIsNewStrategy(java.lang.Class)
	 */
	public IsNewStrategy getIsNewStrategy(Class<?> type) {
		return cache.computeIfAbsent(type, delegate::getIsNewStrategy);
	}
}
