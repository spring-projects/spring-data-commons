/*
 * Copyright 2022-2022 the original author or authors.
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
package org.springframework.data.web.config;

import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ReactiveSortHandlerMethodArgumentResolver} configuration.
 *
 * @author Vedran Pavic
 * @author Oliver Gierke
 * @author Matías Hermosilla
 * @since 3.0
 */
@FunctionalInterface
public interface ReactiveSortHandlerMethodArgumentResolverCustomizer {

	/**
	 * Customize the given {@link ReactiveSortHandlerMethodArgumentResolver}.
	 *
	 * @param sortResolver the {@link ReactiveSortHandlerMethodArgumentResolver} to customize, will never be {@literal null}.
	 */
	void customize(ReactiveSortHandlerMethodArgumentResolver sortResolver);
}
