/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Map;

/**
 * Interface for components that post-process repository configuration of discovered repository interfaces.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 3.0
 */
public interface RepositoryConfigurationPostProcessor {

	/**
	 * Set the repository configuration map that contains repository bean names to {@link RepositoryConfiguration}.
	 *
	 * @param configMap
	 */
	void setConfigMap(Map<String, RepositoryConfiguration<?>> configMap);


	/**
	 * Method to test whether this configuration post-processor can be applied to the given {@link RepositoryConfigurationExtension}.
	 *
	 * @param extension
	 * @return {@code true} if applicable; {@code false} otherwise to skip processing.
	 */
	default boolean supports(RepositoryConfigurationExtension extension) {
		return true;
	}

	/**
	 * Marker interface for default fallback post-processors.
	 */
	interface FallbackRepositoryConfigurationPostProcessor extends RepositoryConfigurationPostProcessor {
	}

}
