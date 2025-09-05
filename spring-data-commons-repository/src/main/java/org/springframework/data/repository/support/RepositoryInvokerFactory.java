/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.repository.support;

/**
 * Interface for a factory to create {@link RepositoryInvoker} instances for repositories managing a particular domain
 * type.
 *
 * @author Oliver Gierke
 * @since 1.10
 */
public interface RepositoryInvokerFactory {

	/**
	 * Returns the {@link RepositoryInvoker} for a repository managing the given domain type.
	 *
	 * @param domainType must not be {@literal null}.
	 * @return
	 */
	RepositoryInvoker getInvokerFor(Class<?> domainType);
}
