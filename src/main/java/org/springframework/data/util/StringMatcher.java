/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.util;

/**
 * Match modes for treatment of {@link String} values.
 *
 * @author Christoph Strobl
 * @author Jens Schauder
 */
public enum StringMatcher {

	/**
	 * Store specific default.
	 */
	DEFAULT,
	/**
	 * Matches the exact string
	 */
	EXACT,
	/**
	 * Matches string starting with pattern
	 */
	STARTING,
	/**
	 * Matches string ending with pattern
	 */
	ENDING,
	/**
	 * Matches string containing pattern
	 */
	CONTAINING,
	/**
	 * Treats strings as regular expression patterns
	 */
	REGEX,

	// might be a temporary thing.
	LIKE

}

