/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import java.util.Map;

/**
 * Interface providing metadata about a query. Name and multiplicity of keys is subject to a provider's implementation.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface QueryMetadata {

	/**
	 * @return serialize query metadata to a {@link Map} of key/value pairs using simple types (string, numbers).
	 */
	Map<String, Object> serialize();

}
