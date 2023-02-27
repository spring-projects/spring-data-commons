/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.domain;

/**
 * Interface to specify a position within a total query result. Scroll positions are used to start scrolling from the
 * beginning of a query result or to resume scrolling from a given position within the query result.
 *
 * @author Mark Paluch
 * @since 3.1
 */
public interface ScrollPosition {

	/**
	 * Returns whether the current scroll position is the initial one.
	 *
	 * @return
	 */
	boolean isInitial();
}
