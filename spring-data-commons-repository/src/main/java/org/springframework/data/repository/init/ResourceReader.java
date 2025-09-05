/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.data.repository.init;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/**
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public interface ResourceReader {

	enum Type {
		XML, JSON;
	}

	/**
	 * Reads a single or {@link java.util.Collection} of target objects from the given {@link Resource}.
	 *
	 * @param resource must not be {@literal null}.
	 * @param classLoader can be {@literal null}.
	 * @return {@link java.util.Collection} of target objects if resource contains multiple ones of single on. Never
	 *         {@literal null}.
	 * @throws Exception
	 */
	Object readFrom(Resource resource, @Nullable ClassLoader classLoader) throws Exception;
}
