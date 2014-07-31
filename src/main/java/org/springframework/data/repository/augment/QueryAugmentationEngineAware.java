/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.repository.augment;

import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * Injection interface to express the dependency to a {@link QueryAugmentationEngine}. Usually implemented by repository
 * implementations. The {@link RepositoryFactorySupport} base class will inject the {@link QueryAugmentationEngine}.
 * 
 * @since 1.9
 * @author Oliver Gierke
 */
public interface QueryAugmentationEngineAware {

	/**
	 * Configures the {@link QueryAugmentationEngine} to be used.
	 * 
	 * @param engine will never be {@literal null}.
	 */
	void setQueryAugmentationEngine(QueryAugmentationEngine engine);
}
