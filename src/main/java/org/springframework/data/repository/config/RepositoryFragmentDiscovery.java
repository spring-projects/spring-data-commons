/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Optional;

import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.util.Streamable;

/**
 * Interface containing the configurable options for the Spring Data repository fragment subsystem.
 * 
 * @author Mark Paluch
 * @since 2.1
 * @see RepositoryConfigurationSource
 */
public interface RepositoryFragmentDiscovery {

	/**
	 * Returns the {@link TypeFilter}s to be used to exclude packages from repository scanning.
	 *
	 * @return
	 */
	Streamable<TypeFilter> getExcludeFilters();

	/**
	 * Returns the configured postfix to be used for looking up custom implementation classes.
	 *
	 * @return the postfix to use or {@link Optional#empty()} in case none is configured.
	 */
	Optional<String> getRepositoryImplementationPostfix();
}
