/*
 * Copyright 2017-present the original author or authors.
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

import org.springframework.data.web.OffsetScrollPositionHandlerMethodArgumentResolver;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link OffsetScrollPositionHandlerMethodArgumentResolver} configuration.
 *
 * @since 2.0
 * @author Yanming Zhou
 */
public interface OffsetScrollPositionHandlerMethodArgumentResolverCustomizer {

	/**
	 * Customize the given {@link OffsetScrollPositionHandlerMethodArgumentResolver}.
	 *
	 * @param offsetResolver the {@link OffsetScrollPositionHandlerMethodArgumentResolver} to customize, will never be {@literal null}.
	 */
	void customize(OffsetScrollPositionHandlerMethodArgumentResolver offsetResolver);
}
